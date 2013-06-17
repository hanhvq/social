/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.storage.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.chromattic.api.query.Query;
import org.chromattic.api.query.QueryBuilder;
import org.chromattic.api.query.QueryResult;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.service.ProcessContext;
import org.exoplatform.social.common.service.utils.ObjectHelper;
import org.exoplatform.social.core.activity.filter.ActivityFilter;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.chromattic.entity.ActivityEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityRef;
import org.exoplatform.social.core.chromattic.entity.ActivityRefListEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.filter.JCRFilterLiteral;
import org.exoplatform.social.core.chromattic.utils.ActivityRefList;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.ActivityStreamStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.exception.NodeNotFoundException;
import org.exoplatform.social.core.storage.query.JCRProperties;
import org.exoplatform.social.core.storage.query.WhereExpression;
import org.exoplatform.social.core.storage.streams.StreamProcessContext;

public class ActivityStreamStorageImpl extends AbstractStorage implements ActivityStreamStorage {
  
  /**
   * The identity storage
   */
  private final IdentityStorageImpl identityStorage;
  
  /**
   * The space storage
   */
  private SpaceStorage spaceStorage;
  

  /**
   * The relationship storage
   */
  private RelationshipStorage relationshipStorage;
  
  /**
   * The activity storage
   */
  private ActivityStorage activityStorage;
  
  /** Logger */
  private static final Log LOG = ExoLogger.getLogger(ActivityStreamStorageImpl.class);
  
  public ActivityStreamStorageImpl(IdentityStorageImpl identityStorage) {
    this.identityStorage = identityStorage;
  }
  
  private ActivityStorage getStorage() {
    if (activityStorage == null) {
      activityStorage = (ActivityStorage) PortalContainer.getInstance().getComponentInstanceOfType(ActivityStorage.class);
    }
    
    return activityStorage;
  }
  
  private SpaceStorage getSpaceStorage() {
    if (spaceStorage == null) {
      spaceStorage = (SpaceStorage) PortalContainer.getInstance().getComponentInstanceOfType(SpaceStorage.class);
    }
    
    return this.spaceStorage;
  }
  
  private RelationshipStorage getRelationshipStorage() {
    if (relationshipStorage == null) {
      relationshipStorage = (RelationshipStorage) PortalContainer.getInstance().getComponentInstanceOfType(RelationshipStorage.class);
    }
    
    return this.relationshipStorage;
  }

  @Override
  public void save(ProcessContext ctx) {
    try {
      StreamProcessContext streamCtx = ObjectHelper.cast(StreamProcessContext.class, ctx);
      Identity owner = streamCtx.getIdentity();
      //
      ActivityEntity activityEntity = _findById(ActivityEntity.class, streamCtx.getActivity().getId());     
      if (OrganizationIdentityProvider.NAME.equals(owner.getProviderId())) {
        user(owner, activityEntity);
      } else if (SpaceIdentityProvider.NAME.equals(owner.getProviderId())) {
        //records to Space Streams for SpaceIdentity
        space(owner, activityEntity);
        
      }
    } catch (NodeNotFoundException e) {
      LOG.warn("Failed to add Activity references.", e);
    }
  }
  
  private void user(Identity owner, ActivityEntity activityEntity) throws NodeNotFoundException {
    createOwnerRefs(owner, activityEntity);

    //
    List<Identity> got = getRelationshipStorage().getConnections(owner);
    if (got.size() > 0) {
      createConnectionsRefs(got, activityEntity);
    }
  }

  private void space(Identity owner, ActivityEntity activityEntity) throws NodeNotFoundException {
    //
    manageRefList(new UpdateContext(owner, null), activityEntity, ActivityRefType.SPACE_STREAM);
    //
    Identity ownerPosterOnSpace = identityStorage.findIdentityById(activityEntity.getPosterIdentity().getId());
    ownerSpaceMembersRefs(ownerPosterOnSpace, activityEntity);
    //
    Space space = getSpaceStorage().getSpaceByPrettyName(owner.getRemoteId());
    
    if (space == null) return;
    //Don't create ActivityRef on space stream for given SpaceIdentity
    List<Identity> identities = getMemberIdentities(space);
    createSpaceMembersRefs(identities, activityEntity);
  }

  private List<Identity> getMemberIdentities(Space space) {
    List<Identity> identities = new ArrayList<Identity>();
    for(String remoteId : space.getMembers()) {
      identities.add(identityStorage.findIdentity(OrganizationIdentityProvider.NAME, remoteId));
    }
    
    return identities;
  }
  
  @Override
  public void delete(String activityId) {
    try {
      //
      ActivityEntity activityEntity = _findById(ActivityEntity.class, activityId);
      
      Collection<ActivityRef> references = activityEntity.getActivityRefs();
      
      List<ActivityRefListEntity> refList = new ArrayList<ActivityRefListEntity>(); 
      //
      for(ActivityRef ref : references) {
        
        //
        refList.add(ref.getDay().getMonth().getYear().getList());
      }
      
      for(ActivityRefListEntity list : refList) {
        list.remove(activityEntity.getLastUpdated());
      }
      
      
    } catch (NodeNotFoundException e) {
      LOG.warn("Failed to delete Activities references.", e);
    }
  }
  
  @Override
  public void unLike(Identity removedLike, ExoSocialActivity activity) {
    try {
      //
      ActivityEntity entity = _findById(ActivityEntity.class, activity.getId());
      
      manageRefList(new UpdateContext(null, removedLike), entity, ActivityRefType.FEED);
      manageRefList(new UpdateContext(null, removedLike), entity, ActivityRefType.MY_ACTIVITIES);
      
      
    } catch (NodeNotFoundException e) {
      LOG.warn("Failed to unLike Activity References");
    }
  }

  @Override
  public void update(ProcessContext ctx) {
    
    try {
      StreamProcessContext streamCtx = ObjectHelper.cast(StreamProcessContext.class, ctx);
      ExoSocialActivity activity = streamCtx.getActivity();
      long oldUpdated = streamCtx.getOldUpdated();

      //
      ActivityEntity activityEntity = _findById(ActivityEntity.class, activity.getId());
      Collection<ActivityRef> references = activityEntity.getActivityRefs();
      
      List<ActivityRefListEntity> refList = new ArrayList<ActivityRefListEntity>(); 
      //
      for(ActivityRef ref : references) {
        if (ref.getLastUpdated() == activity.getUpdated().getTime()) continue;
        
        //
        refList.add(ref.getDay().getMonth().getYear().getList());
      }
      
      //
      for(ActivityRefListEntity list : refList) {
        //LOG.info("update()::BEFORE");
        //printDebug(list, oldUpdated);
        
        list.remove(oldUpdated);
        ActivityRef ref = list.get(activity.getUpdated().getTime());
        ref.setLastUpdated(activity.getUpdated().getTime());
        ref.setActivityEntity(activityEntity);
        
        //LOG.info("update()::AFTER");
        //printDebug(list, oldUpdated);
      }
    } catch (NodeNotFoundException e) {
      LOG.warn("Failed to update Activity references.");
    }
  }
  
  @Override
  public void addSpaceMember(ProcessContext ctx) {
    try {
      
      StreamProcessContext streamCtx = ObjectHelper.cast(StreamProcessContext.class, ctx);
      createSpaceMemberRefs(streamCtx.getIdentity(), streamCtx.getSpaceIdentity());
      
    } catch (NodeNotFoundException e) {
      LOG.warn("Failed to addSpaceMember Activity references.");
    }
    
  }
  
  @Override
  public void removeSpaceMember(ProcessContext ctx) {
    try {
      StreamProcessContext streamCtx = ObjectHelper.cast(StreamProcessContext.class, ctx);
      removeSpaceMemberRefs(streamCtx.getIdentity(), streamCtx.getSpaceIdentity());
    } catch (NodeNotFoundException e) {
      LOG.warn("Failed to removeSpaceMember Activity references.");
    }
    
  }

  

  @Override
  public List<ExoSocialActivity> getFeed(Identity owner, int offset, int limit) {
    return getActivities(ActivityRefType.FEED, owner, offset, limit);
  }

  @Override
  public int getNumberOfFeed(Identity owner) {
    return getNumberOfActivities(ActivityRefType.FEED, owner);
  }

  @Override
  public List<ExoSocialActivity> getConnections(Identity owner, int offset, int limit) {
    return getActivities(ActivityRefType.CONNECTION, owner, offset, limit);
  }

  @Override
  public int getNumberOfConnections(Identity owner) {
    return getNumberOfActivities(ActivityRefType.CONNECTION, owner);
  }

  @Override
  public List<ExoSocialActivity> getMySpaces(Identity owner, int offset, int limit) {
    return getActivities(ActivityRefType.MY_SPACES, owner, offset, limit);
  }

  @Override
  public int getNumberOfMySpaces(Identity owner) {
    return getNumberOfActivities(ActivityRefType.MY_SPACES, owner);
  }
  
  @Override
  public List<ExoSocialActivity> getSpaceStream(Identity owner, int offset, int limit) {
    return getActivities(ActivityRefType.SPACE_STREAM, owner, offset, limit);
  }

  @Override
  public int getNumberOfSpaceStream(Identity owner) {
    return getNumberOfActivities(ActivityRefType.SPACE_STREAM, owner);
  }

  @Override
  public List<ExoSocialActivity> getMyActivities(Identity owner, int offset, int limit) {
    return getActivities(ActivityRefType.MY_ACTIVITIES, owner, offset, limit);
  }

  @Override
  public int getNumberOfMyActivities(Identity owner) {
    return getNumberOfActivities(ActivityRefType.MY_ACTIVITIES, owner);
  }

  @Override
  public void connect(Identity sender, Identity receiver) {
    try {
      //
      QueryResult<ActivityEntity> activities = getActivitiesOfConnections(sender);
      
      
      IdentityEntity receiverEntity = identityStorage._findIdentityEntity(receiver.getProviderId(), receiver.getRemoteId());
      
      if (activities != null) {
        while(activities.hasNext()) {
          ActivityEntity entity = activities.next();
          
          //has on sender stream
          if (isExistingActivityRef(receiverEntity, entity)) continue;
          
          //
          createConnectionsRefs(receiver, entity);
        }
      }
      
      //
      IdentityEntity senderEntity = identityStorage._findIdentityEntity(sender.getProviderId(), sender.getRemoteId());
      activities = getActivitiesOfConnections(receiver);
      if (activities != null) {
        while(activities.hasNext()) {
          ActivityEntity entity = activities.next();

          //has on receiver stream
          if (isExistingActivityRef(senderEntity, entity)) continue;
          
          //
          createConnectionsRefs(sender, entity);
        }
      }
      
    } catch (NodeNotFoundException e) {
      LOG.warn("Failed to add Activity references when create relationship.");
    }
  }
  
  @Override
  public void deleteConnect(Identity sender, Identity receiver) {
    
    try {
      //
      QueryResult<ActivityEntity> activities = getActivitiesOfConnections(sender);
      
      if (activities != null) {
        while(activities.hasNext()) {
          ActivityEntity entity = activities.next();
          removeRelationshipRefs(receiver, entity);
        }
      }
      
      //
      activities = getActivitiesOfConnections(receiver);
      if (activities != null) {
        while(activities.hasNext()) {
          ActivityEntity entity = activities.next();
          removeRelationshipRefs(sender, entity);
        }
      }
    } catch (NodeNotFoundException e) {
      LOG.warn("Failed to delete Activity references when delete relationship.");
    }
  }
  
  /**
   * The reference types.
   */
  public enum ActivityRefType {
    FEED() {
      @Override
      public ActivityRefListEntity refsOf(IdentityEntity identityEntity) {
        return identityEntity.getStreams().getAll();
      }
    },
    CONNECTION() {
      @Override
      public ActivityRefListEntity refsOf(IdentityEntity identityEntity) {
        return identityEntity.getStreams().getConnections();
      }
    },
    MY_SPACES() {
      @Override
      public ActivityRefListEntity refsOf(IdentityEntity identityEntity) {
        return identityEntity.getStreams().getMySpaces();
      }
    },
    SPACE_STREAM() {
      @Override
      public ActivityRefListEntity refsOf(IdentityEntity identityEntity) {
        return identityEntity.getStreams().getSpace();
      }
    },
    MY_ACTIVITIES() {
      @Override
      public ActivityRefListEntity refsOf(IdentityEntity identityEntity) {
        return identityEntity.getStreams().getOwner();
      }
    };

    public abstract ActivityRefListEntity refsOf(IdentityEntity identityEntity);
  }
  
  private List<ExoSocialActivity> getActivities(ActivityRefType type, Identity owner, int offset, int limit) {
    List<ExoSocialActivity> got = new LinkedList<ExoSocialActivity>();
    try {
      IdentityEntity identityEntity = identityStorage._findIdentityEntity(owner.getProviderId(), owner.getRemoteId());
      ActivityRefListEntity refList = type.refsOf(identityEntity);
      ActivityRefList list = new ActivityRefList(refList);
      
      int nb = 0;
      
      Iterator<ActivityRef> it = list.iterator();

      _skip(it, offset);

      while (it.hasNext()) {
        ActivityRef current = it.next();

        //take care in the case, current.getActivityEntity() = null the same ActivityRef, need to remove it out
        if (current.getActivityEntity() == null) {
          current.getDay().getActivityRefs().remove(current.getName());
          continue;
        }
        
        //
        ExoSocialActivity a = getStorage().getActivity(current.getActivityEntity().getId());
            
        got.add(a);
        //LOG.warn(current.toString());
        

        if (++nb == limit) {
          break;
        }

      }
      
    } catch (NodeNotFoundException e) {
      LOG.warn("Failed to getActivities()");
    }
    
    //Collections.reverse(got);
    
    return got;
  }
  
  private int getNumberOfActivities(ActivityRefType type, Identity owner) {
    try {
      IdentityEntity identityEntity = identityStorage._findIdentityEntity(owner.getProviderId(), owner.getRemoteId());
      ActivityRefListEntity refList = type.refsOf(identityEntity);
      
      return refList.getNumber().intValue();
    } catch (NodeNotFoundException e) {
      LOG.warn("Failed to getNumberOfActivities()");
    }
    
    return 0;
  }
  
  
  private QueryResult<ActivityEntity> getActivitiesOfConnections(Identity ownerIdentity) {

    List<Identity> connections = new ArrayList<Identity>();
    
    if (ownerIdentity == null ) {
      return null;
    }
    
    connections.add(ownerIdentity);
    
    //
    ActivityFilter filter = ActivityFilter.newer();

    //
    return getActivitiesOfIdentities(ActivityBuilderWhere.simple().owners(connections), filter, 0, -1);
  }
  
  private QueryResult<ActivityEntity> getActivitiesOfSpace(Identity spaceIdentity) {

    if (spaceIdentity == null) {
      return null;
    }

    //
    ActivityFilter filter = ActivityFilter.space();

    //
    return getActivitiesOfIdentities(ActivityBuilderWhere.space().owners(spaceIdentity), filter, 0, -1);
  }
  
  /**
   * {@inheritDoc}
   */
  private QueryResult<ActivityEntity> getActivitiesOfIdentities(ActivityBuilderWhere where, ActivityFilter filter,
                                                           long offset, long limit) throws ActivityStorageException {
    return getActivitiesOfIdentitiesQuery(where, filter).objects(offset, limit);
  }
  
  
  private Query<ActivityEntity> getActivitiesOfIdentitiesQuery(ActivityBuilderWhere whereBuilder,
                                                               JCRFilterLiteral filter) throws ActivityStorageException {

    QueryBuilder<ActivityEntity> builder = getSession().createQueryBuilder(ActivityEntity.class);

    builder.where(whereBuilder.build(filter));
    whereBuilder.orderBy(builder, filter);

    return builder.get();
  }
  
  private boolean isExistingActivityRef(IdentityEntity identityEntity, ActivityEntity activityEntity) throws NodeNotFoundException {
    return getActivityRefs(identityEntity, activityEntity).size() > 0;
  }
  
  private QueryResult<ActivityRef> getActivityRefs(IdentityEntity identityEntity, ActivityEntity activityEntity) throws NodeNotFoundException {

    QueryBuilder<ActivityRef> builder = getSession().createQueryBuilder(ActivityRef.class);

    WhereExpression whereExpression = new WhereExpression();
    whereExpression.like(JCRProperties.path, identityEntity.getPath() + "/%");
    whereExpression.and().equals(ActivityRef.target, activityEntity.getId());

    builder.where(whereExpression.toString());
    return builder.get().objects();
  }
  
  private void createOwnerRefs(Identity owner, ActivityEntity activityEntity) throws NodeNotFoundException {
    manageRefList(new UpdateContext(owner, null), activityEntity, ActivityRefType.FEED);
    
    if (OrganizationIdentityProvider.NAME.equals(owner.getProviderId())) {
      manageRefList(new UpdateContext(owner, null), activityEntity, ActivityRefType.MY_ACTIVITIES);
    } else if (SpaceIdentityProvider.NAME.equals(owner.getProviderId())) {
      manageRefList(new UpdateContext(owner, null), activityEntity, ActivityRefType.MY_SPACES);
    }
    
  }
  
  private void createConnectionsRefs(List<Identity> identities, ActivityEntity activityEntity) throws NodeNotFoundException {
    manageRefList(new UpdateContext(identities, null), activityEntity, ActivityRefType.FEED, true);
    manageRefList(new UpdateContext(identities, null), activityEntity, ActivityRefType.CONNECTION, true);
  }
  
  private void createConnectionsRefs(Identity identity, ActivityEntity activityEntity) throws NodeNotFoundException {
    manageRefList(new UpdateContext(identity, null), activityEntity, ActivityRefType.FEED, true);
    manageRefList(new UpdateContext(identity, null), activityEntity, ActivityRefType.CONNECTION, true);
  }
  
  private void removeRelationshipRefs(Identity identity, ActivityEntity activityEntity) throws NodeNotFoundException {
    manageRefList(new UpdateContext(null, identity), activityEntity, ActivityRefType.FEED);
    manageRefList(new UpdateContext(null, identity), activityEntity, ActivityRefType.CONNECTION);
  }

  private void createSpaceMembersRefs(List<Identity> identities, ActivityEntity activityEntity) throws NodeNotFoundException {
    manageRefList(new UpdateContext(identities, null), activityEntity, ActivityRefType.FEED);
    manageRefList(new UpdateContext(identities, null), activityEntity, ActivityRefType.MY_SPACES);
    //manageRefList(new UpdateContext(identities, null), activityEntity, ActivityRefType.SPACE_STREAM);
  }
  
  private void ownerSpaceMembersRefs(Identity identity, ActivityEntity activityEntity) throws NodeNotFoundException {
    manageRefList(new UpdateContext(identity, null), activityEntity, ActivityRefType.MY_ACTIVITIES);
  }
  
  private void createSpaceMembersRefs(Identity identity, ActivityEntity activityEntity) throws NodeNotFoundException {
    manageRefList(new UpdateContext(identity, null), activityEntity, ActivityRefType.FEED);
    manageRefList(new UpdateContext(identity, null), activityEntity, ActivityRefType.MY_SPACES);
  }
  
  private void createSpaceMemberRefs(Identity member, Identity space) throws NodeNotFoundException {
    QueryResult<ActivityEntity> spaceActivities = getActivitiesOfSpace(space);
    if (spaceActivities != null) {
      while(spaceActivities.hasNext()) {
        createSpaceMembersRefs(member, spaceActivities.next());
      }
    }
    
  }
  
  private void removeSpaceMemberRefs(Identity removedMember, Identity space) throws NodeNotFoundException {
    QueryResult<ActivityEntity> spaceActivities = getActivitiesOfSpace(space);
    if (spaceActivities != null) {
      while(spaceActivities.hasNext()) {
        
        ActivityEntity entity = spaceActivities.next();
        manageRefList(new UpdateContext(null, removedMember), entity, ActivityRefType.FEED);
        manageRefList(new UpdateContext(null, removedMember), entity, ActivityRefType.MY_SPACES);
      }
    }
    
  }
  
  private void manageRefList(UpdateContext context, ActivityEntity activityEntity, ActivityRefType type) throws NodeNotFoundException {
    manageRefList(context, activityEntity, type, false);
  }
  
  private void manageRefList(UpdateContext context, ActivityEntity activityEntity, ActivityRefType type, boolean mustCheck) throws NodeNotFoundException {

    if (context.getAdded() != null) {
      for (Identity identity : context.getAdded()) {
        IdentityEntity identityEntity = identityStorage._findIdentityEntity(identity.getProviderId(), identity.getRemoteId());

        //
        if (mustCheck) {
          //to avoid add back activity to given stream what has already existing
          if (isExistingActivityRef(identityEntity, activityEntity)) continue;
        }
        
        
        ActivityRefListEntity listRef = type.refsOf(identityEntity);
        ActivityRef ref = listRef.get(activityEntity);
        //LOG.info("manageRefList()::BEFORE");
        //printDebug(listRef, activityEntity.getLastUpdated());
        if (ref.getName() == null) {
          ref.setName(activityEntity.getName());
        }

        if (ref.getLastUpdated() == null) {
          ref.setLastUpdated(activityEntity.getLastUpdated());
        }

        ref.setActivityEntity(activityEntity);

        //LOG.info("manageRefList()::AFTER");
        //printDebug(listRef, activityEntity.getLastUpdated());
      }
    }
    
    if (context.getRemoved() != null) {

      for (Identity identity : context.getRemoved()) {
        IdentityEntity identityEntity = identityStorage._findIdentityEntity(identity.getProviderId(), identity.getRemoteId());
          
        ActivityRefListEntity listRef = type.refsOf(identityEntity);
        listRef.remove(activityEntity);
      }
    }
  }
  
  private void printDebug(ActivityRefListEntity list, long oldUpdated) {
    LOG.info("printDebug::SIZE = " + list.refs(oldUpdated).size());
    LOG.info("printDebug::path = " + list.getPath());
    
    Map<String, ActivityRef> refs = list.refs(oldUpdated);
    for(Entry<String, ActivityRef> entry : refs.entrySet()) {
      if (entry.getValue() != null && entry.getValue().getActivityEntity() != null )
      LOG.info(String.format("printDebug::KEY = %s| %s", entry.getKey(), entry.getValue().toString()));
    }
  }
  
  private class UpdateContext {
    private List<Identity> added;
    private List<Identity> removed;

    private UpdateContext(List<Identity> added, List<Identity> removed) {
      this.added = added;
      this.removed = removed;
    }
    
    private UpdateContext(Identity added, Identity removed) {
      if (added != null) {
        this.added = new CopyOnWriteArrayList<Identity>();
        this.added.add(added);
      }
      
      //
      if (removed != null) {
        this.removed = new CopyOnWriteArrayList<Identity>();
        this.removed.add(removed);
      }
    }

    public List<Identity> getAdded() {
      return added == null ? new ArrayList<Identity>() : added;
    }

    public List<Identity> getRemoved() {
      return removed == null ? new ArrayList<Identity>() : removed;
    }
  }

}