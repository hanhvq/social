package org.exoplatform.social.core.storage.mongodb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;
import org.chromattic.api.ChromatticException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.activity.filter.ActivityFilter;
import org.exoplatform.social.core.activity.filter.ActivityUpdateFilter;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ActivityStreamImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.chromattic.entity.ActivityEntity;
import org.exoplatform.social.core.chromattic.entity.HidableEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.mongo.entity.ActivityMongoEntity;
import org.exoplatform.social.core.mongo.entity.CommentMongoEntity;
import org.exoplatform.social.core.mongo.entity.ActivityRefMongoEntity;
import org.exoplatform.social.core.mongo.entity.ActivityRefMongoEntity.ViewerType;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.ActivityStreamStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.exception.NodeNotFoundException;
import org.exoplatform.social.core.storage.impl.ActivityBuilderWhere;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.exoplatform.social.core.storage.streams.StreamInvocationHelper;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class ActivityMongoStorageImpl extends AbstractMongoStorage implements ActivityStorage {
  
  /** .. */
  private static final Log LOG = ExoLogger.getLogger(ActivityMongoStorageImpl.class);
  /** .. */
  private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s]+)|@([^\\s]+)$");
  /** .. */
  public static final Pattern USER_NAME_VALIDATOR_REGEX = Pattern.compile("^[\\p{L}][\\p{L}._\\-\\d]+$");
  /** .. */
  private static final String ACTIVITY_COLLECTION_NAME = "activity";
  /** .. */
  private static final String COMMENT_COLLECTION_NAME = "comment";
  /** .. */
  private static final String ALL_STREAM_COLLECTION_NAME = "all";
  /** .. */
  private static final String CONNECTION_STREAM_COLLECTION_NAME = "connection";
  /** .. */
  private static final String OWNER_STREAM_COLLECTION_NAME = "owner";
  /** .. */
  private static final String SPACE_STREAM_COLLECTION_NAME = "space";
  
  private ActivityStorage activityStorage;

  private final SortedSet<ActivityProcessor> activityProcessors;

  private final RelationshipStorage relationshipStorage;
  private final IdentityStorage identityStorage;
  private final SpaceStorage spaceStorage;
  private final ActivityStreamStorage streamStorage;
  //sets value to tell this storage to inject Streams or not

  public ActivityMongoStorageImpl(
      final RelationshipStorage relationshipStorage,
      final IdentityStorage identityStorage,
      final SpaceStorage spaceStorage,
      final ActivityStreamStorage streamStorage,
      final MongoStorage mongoStorage) {
    
    super(mongoStorage);
    this.relationshipStorage = relationshipStorage;
    this.identityStorage = identityStorage;
    this.spaceStorage = spaceStorage;
    this.streamStorage = streamStorage;
    this.activityProcessors = new TreeSet<ActivityProcessor>(processorComparator());
  }
  
  
  private static Comparator<ActivityProcessor> processorComparator() {
    return new Comparator<ActivityProcessor>() {

      public int compare(ActivityProcessor p1, ActivityProcessor p2) {
        if (p1 == null || p2 == null) {
          throw new IllegalArgumentException("Cannot compare null ActivityProcessor");
        }
        return p1.getPriority() - p2.getPriority();
      }
    };
  }

	@Override
	public void setInjectStreams(boolean mustInject) {
		
	}

	@Override
  public ExoSocialActivity getActivity(String activityId) throws ActivityStorageException {
	  //
    DBCollection collection = getCollection(ACTIVITY_COLLECTION_NAME);
    BasicDBObject query = new BasicDBObject();
    query.append("_id", new ObjectId(activityId));
    
    BasicDBObject entity = (BasicDBObject) collection.findOne(query);
    
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    
    fillActivity(activity, entity);
    
    return activity;
  }

  @Override
  public List<ExoSocialActivity> getUserActivities(Identity owner) throws ActivityStorageException {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getUserActivities(Identity owner, long offset, long limit) throws ActivityStorageException {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getUserActivitiesForUpgrade(Identity owner, long offset, long limit) throws ActivityStorageException {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivities(Identity owner,
                                               Identity viewer,
                                               long offset,
                                               long limit) throws ActivityStorageException {
    return null;
  }

	@Override
	public void saveComment(ExoSocialActivity activity,
			ExoSocialActivity comment) throws ActivityStorageException {
		
	  try {
	    //
	    DBCollection commentColl = getCollection(COMMENT_COLLECTION_NAME);
	    
      //
      long currentMillis = System.currentTimeMillis();
      long commentMillis = (comment.getPostedTime() != null ? comment.getPostedTime() : currentMillis);
      
      long oldTime = activity.getPostedTime();
      updateActivityRef(activity.getId(), commentMillis, oldTime);
      
      //
      BasicDBObject commentEntity = new BasicDBObject();
      commentEntity.append(CommentMongoEntity.activityId.getName(), activity.getId());
      commentEntity.append(CommentMongoEntity.title.getName(), comment.getTitle());
      commentEntity.append(CommentMongoEntity.titleId.getName(), comment.getTitleId());
      commentEntity.append(CommentMongoEntity.body.getName(), comment.getBody());
      commentEntity.append(CommentMongoEntity.bodyId.getName(), comment.getBodyId());
      commentEntity.append(CommentMongoEntity.poster.getName(), comment.getUserId());
      commentEntity.append(CommentMongoEntity.postedTime.getName(), commentMillis);
      commentEntity.append(CommentMongoEntity.lastUpdated.getName(), commentMillis);
      commentEntity.append(CommentMongoEntity.hidable.getName(), comment.isHidden());
      commentEntity.append(CommentMongoEntity.lockable.getName(), comment.isLocked());
      
      commentColl.insert(commentEntity);
      
      comment.setId(commentEntity.getString("_id") != null ? commentEntity.getString("_id") : null);
      //
//      if (mustInjectStreams) {
//        Identity identity = identityStorage.findIdentityById(comment.getUserId());
//        StreamInvocationHelper.updateCommenter(identity, activity, commenters.toArray(new String[0]), oldUpdated);
//        StreamInvocationHelper.update(activity, mentioners.toArray(new String[0]), oldUpdated);
//      }
    } catch (MongoException ex) {
      throw new ActivityStorageException(ActivityStorageException.Type.FAILED_TO_SAVE_COMMENT, ex.getMessage());
    }
    //
    LOG.debug(String.format(
        "Comment %s by %s (%s) created",
        comment.getTitle(),
        comment.getUserId(),
        comment.getId()
    ));
    
	}
	
	private void updateActivityRef(String activityId, long time, long oldTime) {
	  DBCollection activityColl = getCollection(ACTIVITY_COLLECTION_NAME);
    BasicDBObject update = new BasicDBObject();
    BasicDBObject set = new BasicDBObject();
    set.append(ActivityMongoEntity.lastUpdated.getName(), time);
    //
    update.append("$set", set);
    BasicDBObject query = new BasicDBObject();
    query.put(ActivityMongoEntity.id.getName(), new ObjectId(activityId));
    
    WriteResult result = activityColl.update(query, update);
    LOG.warn("==============>UPDATED ACTIVITY REF: "+result.toString());
    
    //update refs
    DBCollection feedColl = getCollection(ALL_STREAM_COLLECTION_NAME);
    DBCollection connectionColl = getCollection(CONNECTION_STREAM_COLLECTION_NAME);
    DBCollection ownerColl = getCollection(OWNER_STREAM_COLLECTION_NAME);
    query = new BasicDBObject();
    query.put(ActivityRefMongoEntity.activityId.getName(), activityId);
    set = new BasicDBObject();
    set.append(ActivityRefMongoEntity.time.getName(), time);
    update = new BasicDBObject("$set", set);
    
    result = feedColl.update(query, update);
    LOG.warn("==============>UPDATED ACTIVITY Reference for FEED: "+result.toString());
    
    result = connectionColl.update(query, update);
    LOG.warn("==============>UPDATED ACTIVITY Reference for Connection: "+result.toString());
    
    result = ownerColl.update(query, update);
    LOG.warn("==============>UPDATED ACTIVITY Reference for OWNER: "+result.toString());
	}
	
	@Override
	public ExoSocialActivity saveActivity(Identity owner, ExoSocialActivity activity) throws ActivityStorageException {
	  try {
      Validate.notNull(owner, "owner must not be null.");
      Validate.notNull(activity, "activity must not be null.");
      Validate.notNull(activity.getUpdated(), "Activity.getUpdated() must not be null.");
      Validate.notNull(activity.getPostedTime(), "Activity.getPostedTime() must not be null.");
      Validate.notNull(activity.getTitle(), "Activity.getTitle() must not be null.");
    } catch (IllegalArgumentException e) {
      throw new ActivityStorageException(ActivityStorageException.Type.ILLEGAL_ARGUMENTS, e.getMessage(), e);
    }
	  //
	  try {
      _createActivity(owner, activity);
    } catch (MongoException e) {
      LOG.warn("Insert activity failed.", e);
    } catch (NodeNotFoundException e) {
      LOG.warn("Insert activity failed.", e);
    }
	  
    return activity;
	}
	
	protected void _saveActivity(ExoSocialActivity activity) throws NodeNotFoundException {

    ActivityEntity activityEntity = _findById(ActivityEntity.class, activity.getId());
    
    //
    String[] removedLikes = StorageUtils.sub(activityEntity.getLikes(), activity.getLikeIdentityIds());
    String[] addedLikes = StorageUtils.sub(activity.getLikeIdentityIds(), activityEntity.getLikes());
    //Don't update the last modified for activity.
    //Don't apply What's hot for like/dislike case.
    
    if (removedLikes.length > 0 || addedLikes.length > 0) {
      //process likes activity
      //manageActivityLikes(addedLikes, removedLikes, activity);
    }
    //
    //fillActivityEntityFromActivity(activity, activityEntity);
    
  }
	
	/*
   * Private
   */
  private void fillActivityEntityFromActivity(Identity owner, ExoSocialActivity activity, BasicDBObject activityEntity, boolean isNew) {
    //
    activityEntity.append(ActivityMongoEntity.title.getName(), activity.getTitle());
    activityEntity.append(ActivityMongoEntity.titleId.getName(), activity.getTitleId());
    activityEntity.append(ActivityMongoEntity.body.getName() ,activity.getBody());
    activityEntity.append(ActivityMongoEntity.bodyId.getName(), activity.getBodyId());
    
    // Create activity
    long currentMillis = System.currentTimeMillis();
    long activityMillis = (activity.getPostedTime() != null ? activity.getPostedTime() : currentMillis);
    
    activityEntity.append(ActivityMongoEntity.postedTime.getName(), activityMillis);
    activityEntity.append(ActivityMongoEntity.lastUpdated.getName(), activityMillis);
    
    activityEntity.append(ActivityMongoEntity.permaLink.getName(),  activity.getPermaLink());
    
    //mentioners
    List<String> mentioners = new ArrayList<String>();
    activity.setMentionedIds(processMentions(activity.getMentionedIds(), activity.getTitle(), mentioners, true));
    activityEntity.append(ActivityMongoEntity.mentioners.getName(), activity.getMentionedIds());
    
    activityEntity.append(ActivityMongoEntity.likers.getName(), activity.getLikeIdentityIds());
    
    activityEntity.append(ActivityMongoEntity.hidable.getName(),  activity.isHidden());
    activityEntity.append(ActivityMongoEntity.lockable.getName(),  activity.isLocked());
    
    activityEntity.append(ActivityMongoEntity.appId.getName(), activity.getAppId());
    activityEntity.append(ActivityMongoEntity.externalId.getName(), activity.getExternalId());
    
    // Fill activity model
    try {
      if (isNew) {
        IdentityEntity identityEntity = _findById(IdentityEntity.class, owner.getId());
        activity.setStreamOwner(identityEntity.getRemoteId());
        activity.setPostedTime(activityMillis);
        activity.setReplyToId(new String[]{});
        //poster
        String posterId = activity.getUserId() != null ? activity.getUserId() : owner.getId();
        activityEntity.append(ActivityMongoEntity.poster.getName(), posterId);
        activityEntity.append(ActivityMongoEntity.owner.getName(), owner.getId());
        activity.setPosterId(posterId);
      }
    } catch (NodeNotFoundException e) {
      LOG.debug("Could not found identity "+ owner.getId());
    }
    
    activity.setUpdated(activityMillis);
  }
  
  /*
   * Private
   */
  private void fillActivity(ExoSocialActivity activity, BasicDBObject activityEntity) {

    activity.setId(activityEntity.getString(ActivityMongoEntity.id.getName()));
    activity.setTitle(activityEntity.getString(ActivityMongoEntity.title.getName()));
    activity.setTitleId(activityEntity.getString(ActivityMongoEntity.titleId.getName()));
    activity.setBody(activityEntity.getString(ActivityMongoEntity.body.getName()));
    activity.setBodyId(activityEntity.getString(ActivityMongoEntity.bodyId.getName()));
    
    activity.setPosterId(activityEntity.getString(ActivityMongoEntity.poster.getName()));
    activity.setUserId(activityEntity.getString(ActivityMongoEntity.owner.getName()));
    activity.setPermanLink(activityEntity.getString(ActivityMongoEntity.permaLink.getName()));
    BasicBSONList likers = (BasicBSONList) activityEntity.get(ActivityMongoEntity.likers.getName());
    activity.setLikeIdentityIds(likers != null ? likers.toArray(new String[0]) : new String[0]);
    
    BasicBSONList mentions = (BasicBSONList) activityEntity.get(ActivityMongoEntity.mentioners.getName());
    activity.setMentionedIds(mentions != null ? mentions.toArray(new String[0]) : new String[0]);

    activity.isHidden(activityEntity.getBoolean(ActivityMongoEntity.hidable.getName()));
    activity.isLocked(activityEntity.getBoolean(ActivityMongoEntity.lockable.getName()));
    
    activity.setPostedTime(activityEntity.getLong(ActivityMongoEntity.postedTime.getName()));
    activity.setUpdated(activityEntity.getLong(ActivityMongoEntity.lastUpdated.getName()));
    
    activity.setAppId(activityEntity.getString(ActivityMongoEntity.appId.getName()));
    activity.setExternalId(activityEntity.getString(ActivityMongoEntity.externalId.getName()));
    
  }
  
  private void fillStream(ActivityEntity activityEntity, ExoSocialActivity activity) {

    //
    ActivityStream stream = new ActivityStreamImpl();
    
    IdentityEntity identityEntity = null;

    //update new stream owner
    try {
      Identity streamOwnerIdentity = identityStorage.findIdentity(SpaceIdentityProvider.NAME, activity.getStreamOwner());
      if (streamOwnerIdentity == null) {
        streamOwnerIdentity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, activity.getStreamOwner());
      }
      IdentityEntity streamOwnerEntity = _findById(IdentityEntity.class, streamOwnerIdentity.getId());
      identityEntity = streamOwnerEntity;
      activityEntity.setIdentity(streamOwnerEntity);
    } catch (Exception e) {
      identityEntity = activityEntity.getIdentity();
    }
    //
    stream.setId(identityEntity.getId());
    stream.setPrettyId(identityEntity.getRemoteId());
    stream.setType(identityEntity.getProviderId());
    
    //Identity identity = identityStorage.findIdentityById(identityEntity.getId());
    if (identityEntity != null && SpaceIdentityProvider.NAME.equals(identityEntity.getProviderId())) {
      Space space = spaceStorage.getSpaceByPrettyName(identityEntity.getRemoteId());
      //work-around for SOC-2366 when rename space's display name.
      if (space != null) {
        String groupId = space.getGroupId().split("/")[2];
        stream.setPermaLink(LinkProvider.getActivityUriForSpace(identityEntity.getRemoteId(), groupId));
      }
    } else {
      stream.setPermaLink(LinkProvider.getActivityUri(identityEntity.getProviderId(), identityEntity.getRemoteId()));
    }
    //
    activity.setActivityStream(stream);
    activity.setStreamId(stream.getId());
    activity.setStreamOwner(stream.getPrettyId());

  }
	
	/*
   * Internal
   */
  protected String[] _createActivity(Identity owner, ExoSocialActivity activity) throws MongoException, NodeNotFoundException {
    //
    DBCollection collection = getCollection(ACTIVITY_COLLECTION_NAME);
    
    BasicDBObject activityEntity = new BasicDBObject();
    fillActivityEntityFromActivity(owner, activity, activityEntity, true);
    //
    collection.insert(activityEntity);
    activity.setId(activityEntity.getString("_id") != null ? activityEntity.getString("_id") : null);
    
    LOG.warn("==========> ACTIVITY ID:"+ activity.getId());
    
    //fill streams
    createStreamItems(owner, activity);
    
    return activity.getMentionedIds();
  }
  
  private void createStreamItems(Identity owner, ExoSocialActivity activity) {
    //create StreamItem
    if (OrganizationIdentityProvider.NAME.equals(owner.getProviderId())) {
      //poster
      createStreamItemForPoster(owner, activity);
      //connection
      createStreamItemForConnections(owner, activity);
      //mention
      createStreamItemForMentioner(owner, activity);
    } else {
      //for SPACE

    }
  }
  
  private void createStreamItemForPoster(Identity owner, ExoSocialActivity activity) throws MongoException {
    DBCollection feedColl = getCollection(ALL_STREAM_COLLECTION_NAME);
    DBCollection ownerColl = getCollection(OWNER_STREAM_COLLECTION_NAME);
    //poster
    BasicDBObject streamItem = new BasicDBObject();
    fillStreamItem(owner, activity, streamItem);
    streamItem.append(ActivityRefMongoEntity.viewerId.getName(), owner.getId());
    streamItem.append(ActivityRefMongoEntity.time.getName(), activity.getPostedTime());
    feedColl.insert(streamItem);
    
    //
    streamItem = new BasicDBObject();
    fillStreamItem(owner, activity, streamItem);
    streamItem.append(ActivityRefMongoEntity.viewerId.getName(), owner.getId());
    streamItem.append(ActivityRefMongoEntity.time.getName(), activity.getPostedTime());
    ownerColl.insert(streamItem);
  }
  
  private void createStreamItemForMentioner(Identity owner, ExoSocialActivity activity) throws MongoException {
    DBCollection ownerColl = getCollection(OWNER_STREAM_COLLECTION_NAME);
    DBCollection feedColl = getCollection(ALL_STREAM_COLLECTION_NAME);
    //
    for (String mentioner : activity.getMentionedIds()) {
      BasicDBObject mentionerStreamItem = new BasicDBObject();
      fillStreamItem(owner, activity, mentionerStreamItem);
      mentionerStreamItem.append(ActivityRefMongoEntity.viewerId.getName(), mentioner);
      mentionerStreamItem.append(ActivityRefMongoEntity.viewerTypes.getName(), ViewerType.MENTIONER.name());
      mentionerStreamItem.append(ActivityRefMongoEntity.time.getName(), activity.getPostedTime());
      ownerColl.insert(mentionerStreamItem);
      feedColl.insert(mentionerStreamItem);
    }
  }
  
  private void createStreamItemForConnections(Identity owner, ExoSocialActivity activity) throws MongoException {
    List<Identity> relationships = relationshipStorage.getConnections(owner);
    DBCollection feedColl = getCollection(ALL_STREAM_COLLECTION_NAME);
    DBCollection connColl = getCollection(CONNECTION_STREAM_COLLECTION_NAME);
    //
    for (Identity identity : relationships) {
      BasicDBObject streamItemEntity = new BasicDBObject();
      fillStreamItem(owner, activity, streamItemEntity);
      streamItemEntity.append(ActivityRefMongoEntity.viewerTypes.getName(), ViewerType.CONNECTION.name());
      streamItemEntity.append(ActivityRefMongoEntity.viewerId.getName(), identity.getId());
      streamItemEntity.append(ActivityRefMongoEntity.time.getName(), activity.getPostedTime());
      //
      connColl.insert(streamItemEntity);
      feedColl.insert(streamItemEntity);
    }
  }
  
  private void fillStreamItem(Identity owner, ExoSocialActivity activity, BasicDBObject streamItemEntity) {
    //
    streamItemEntity.append(ActivityRefMongoEntity.activityId.getName(), activity.getId());
    streamItemEntity.append(ActivityRefMongoEntity.owner.getName(), owner.getId());
    streamItemEntity.append(ActivityRefMongoEntity.poster.getName(), activity.getUserId() != null ? activity.getUserId() : owner.getId());
    streamItemEntity.append(ActivityRefMongoEntity.hiable.getName(), activity.isHidden());
    streamItemEntity.append(ActivityRefMongoEntity.lockable.getName(), activity.isLocked());
  }
  
  /**
   * Processes Mentioners who has been mentioned via the Activity.
   * 
   * @param mentionerIds
   * @param title
   * @param isAdded
   * @return list of added IdentityIds who mentioned
   */
  private String[] processMentions(String[] mentionerIds, String title, List<String> addedOrRemovedIds, boolean isAdded) {
    if (title == null || title.length() == 0) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }
    
    Matcher matcher = MENTION_PATTERN.matcher(title);
    while (matcher.find()) {
      String remoteId = matcher.group().substring(1);
      if (!USER_NAME_VALIDATOR_REGEX.matcher(remoteId).matches()) {
        continue;
      }
      Identity identity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, remoteId);
      // if not the right mention then ignore
      if (identity != null) { 
        String mentionStr = identity.getId() + MENTION_CHAR; // identityId@
        mentionerIds = isAdded ? add(mentionerIds, mentionStr, addedOrRemovedIds) : remove(mentionerIds, mentionStr, addedOrRemovedIds);
      }
    }
    return mentionerIds;
  }
  
  private String[] add(String[] mentionerIds, String mentionStr, List<String> addedOrRemovedIds) {
    if (ArrayUtils.toString(mentionerIds).indexOf(mentionStr) == -1) { // the first mention
      addedOrRemovedIds.add(mentionStr.replace(MENTION_CHAR, ""));
      return (String[]) ArrayUtils.add(mentionerIds, mentionStr + 1);
    }
    
    String storedId = null;
    for (String mentionerId : mentionerIds) {
      if (mentionerId.indexOf(mentionStr) != -1) {
        mentionerIds = (String[]) ArrayUtils.removeElement(mentionerIds, mentionerId);
        storedId = mentionStr + (Integer.parseInt(mentionerId.split(MENTION_CHAR)[1]) + 1);
        break;
      }
    }
    

    addedOrRemovedIds.add(mentionStr.replace(MENTION_CHAR, ""));
    mentionerIds = (String[]) ArrayUtils.add(mentionerIds, storedId);
    return mentionerIds;
  }

  private String[] remove(String[] mentionerIds, String mentionStr, List<String> addedOrRemovedIds) {
    for (String mentionerId : mentionerIds) {
      if (mentionerId.indexOf(mentionStr) != -1) {
        int numStored = Integer.parseInt(mentionerId.split(MENTION_CHAR)[1]) - 1;
        
        if (numStored == 0) {
          addedOrRemovedIds.add(mentionStr.replace(MENTION_CHAR, ""));
          return (String[]) ArrayUtils.removeElement(mentionerIds, mentionerId);
        }

        mentionerIds = (String[]) ArrayUtils.removeElement(mentionerIds, mentionerId);
        mentionerIds = (String[]) ArrayUtils.add(mentionerIds, mentionStr + numStored);
        addedOrRemovedIds.add(mentionStr.replace(MENTION_CHAR, ""));
        break;
      }
    }
    return mentionerIds;
  }

  @Override
  public ExoSocialActivity getParentActivity(ExoSocialActivity comment) throws ActivityStorageException {
    return null;
  }

  @Override
  public void deleteActivity(String activityId) throws ActivityStorageException {
    //
    DBCollection collection = getCollection(ACTIVITY_COLLECTION_NAME);
    BasicDBObject query = new BasicDBObject();
    query.append("_id", new ObjectId(activityId));
    
    WriteResult result = collection.remove(query);
    LOG.warn("==========>DELETED: "+result);
    deleteActivityRef(activityId);
  }
  
  private void deleteActivityRef(String activityId) {
    //
    DBCollection feedColl = getCollection(ALL_STREAM_COLLECTION_NAME);
    DBCollection connColl = getCollection(CONNECTION_STREAM_COLLECTION_NAME);
    DBCollection ownerColl = getCollection(OWNER_STREAM_COLLECTION_NAME);
    BasicDBObject query = new BasicDBObject();
    query.append(ActivityRefMongoEntity.activityId.getName(), activityId);
    
    WriteResult result = feedColl.remove(query);
    LOG.warn("==========>FEED REF DELETED: "+result);
    result = connColl.remove(query);
    LOG.warn("==========>CONNECTION REF DELETED: "+result);
    result = ownerColl.remove(query);
    LOG.warn("==========>OWNER REF DELETED: "+result);
  }

  @Override
  public void deleteComment(String activityId, String commentId) throws ActivityStorageException {
		
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentities(
			List<Identity> connectionList, long offset, long limit)
			throws ActivityStorageException {
		return null;
	}

	@Override
	public List<ExoSocialActivity> getActivitiesOfIdentities(
			List<Identity> connectionList, TimestampType type, long offset,
			long limit) throws ActivityStorageException {
		return null;
	}

	@Override
	public int getNumberOfUserActivities(Identity owner)
			throws ActivityStorageException {
		return 0;
	}

	@Override
	public int getNumberOfUserActivitiesForUpgrade(Identity owner)
			throws ActivityStorageException {
		return 0;
	}

	@Override
	public int getNumberOfNewerOnUserActivities(Identity ownerIdentity,
			ExoSocialActivity baseActivity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ExoSocialActivity> getNewerOnUserActivities(
			Identity ownerIdentity, ExoSocialActivity baseActivity, int limit) {
		return null;
	}

  @Override
  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnUserActivities(Identity ownerIdentity,
                                                          ExoSocialActivity baseActivity,
                                                          int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivityFeed(Identity ownerIdentity, int offset, int limit) {
    //
    DBCollection feedColl = getCollection(ALL_STREAM_COLLECTION_NAME);
    DBCollection activityColl = getCollection(ACTIVITY_COLLECTION_NAME);
    BasicDBObject query = new BasicDBObject(ActivityRefMongoEntity.viewerId.getName(), ownerIdentity.getId());
    BasicDBObject sortObj = new BasicDBObject("time", -1);

    DBCursor cur = feedColl.find(query).sort(sortObj).skip(offset).limit(limit);
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    while (cur.hasNext()) {
      BasicDBObject row = (BasicDBObject) cur.next();
      String activityId = row.getString(ActivityRefMongoEntity.activityId.getName());
      BasicDBObject entity = (BasicDBObject) activityColl.findOne(new BasicDBObject("_id", new ObjectId(activityId)));
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      fillActivity(activity, entity);
      result.add(activity);
    }
    
    return result;
  }

  @Override
  public List<ExoSocialActivity> getActivityFeedForUpgrade(Identity ownerIdentity,
                                                           int offset,
                                                           int limit) {
    return null;
  }

  @Override
  public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity) {
    //
    DBCollection feedColl = getCollection(ALL_STREAM_COLLECTION_NAME);
    return feedColl.find().size();
  }

  @Override
  public int getNumberOfActivitesOnActivityFeedForUpgrade(Identity ownerIdentity) {
    return 0;
  }

  @Override
  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnActivityFeed(Identity ownerIdentity,
                                                        ExoSocialActivity baseActivity,
                                                        int limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnActivityFeed(Identity ownerIdentity,
                                                        ExoSocialActivity baseActivity,
                                                        int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity,
                                                            int offset,
                                                            int limit) {
    //
    DBCollection connectionColl = getCollection(CONNECTION_STREAM_COLLECTION_NAME);
    DBCollection activityColl = getCollection(ACTIVITY_COLLECTION_NAME);
    BasicDBObject sortObj = new BasicDBObject("time", -1);

    DBCursor cur = connectionColl.find().sort(sortObj).skip(offset).limit(limit);
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    while (cur.hasNext()) {
      BasicDBObject row = (BasicDBObject) cur.next();
      String activityId = row.getString(ActivityRefMongoEntity.activityId.getName());
      BasicDBObject entity = (BasicDBObject) activityColl.findOne(new BasicDBObject("_id", new ObjectId(activityId)));
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      fillActivity(activity, entity);
      result.add(activity);
    }
    
    return result;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnectionsForUpgrade(Identity ownerIdentity,
                                                                      int offset,
                                                                      int limit) {
    return null;
  }

  @Override
  public int getNumberOfActivitiesOfConnections(Identity ownerIdentity) {
    return 0;
  }

  @Override
  public int getNumberOfActivitiesOfConnectionsForUpgrade(Identity ownerIdentity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentity(Identity ownerIdentity,
                                                         long offset,
                                                         long limit) {
    return null;
  }

  @Override
  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity,
                                                       ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnActivitiesOfConnections(Identity ownerIdentity,
                                                                   ExoSocialActivity baseActivity,
                                                                   long limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity,
                                                       ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnActivitiesOfConnections(Identity ownerIdentity,
                                                                   ExoSocialActivity baseActivity,
                                                                   int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getUserSpacesActivities(Identity ownerIdentity,
                                                         int offset,
                                                         int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getUserSpacesActivitiesForUpgrade(Identity ownerIdentity,
                                                                   int offset,
                                                                   int limit) {
    return null;
  }

  @Override
  public int getNumberOfUserSpacesActivities(Identity ownerIdentity) {
    return 0;
  }

  @Override
  public int getNumberOfUserSpacesActivitiesForUpgrade(Identity ownerIdentity) {
    return 0;
  }

  @Override
  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity,
                                                    ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnUserSpacesActivities(Identity ownerIdentity,
                                                                ExoSocialActivity baseActivity,
                                                                int limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity,
                                                    ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnUserSpacesActivities(Identity ownerIdentity,
                                                                ExoSocialActivity baseActivity,
                                                                int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getComments(ExoSocialActivity existingActivity,
                                             int offset,
                                             int limit) {
    DBCollection activityColl = getCollection(COMMENT_COLLECTION_NAME);
    BasicDBObject query = new BasicDBObject(CommentMongoEntity.activityId.getName(), existingActivity.getId());

    DBCursor cur = activityColl.find(query);
    List<ExoSocialActivity> result = new ArrayList<ExoSocialActivity>();
    while (cur.hasNext()) {
      BasicDBObject entity = (BasicDBObject) cur.next();
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      fillActivity(activity, entity);
      activity.isComment(true);
      result.add(activity);
    }
    
    return result;
  }

  @Override
  public int getNumberOfComments(ExoSocialActivity existingActivity) {
    return 0;
  }

  @Override
  public int getNumberOfNewerComments(ExoSocialActivity existingActivity,
                                      ExoSocialActivity baseComment) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerComments(ExoSocialActivity existingActivity,
                                                  ExoSocialActivity baseComment,
                                                  int limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderComments(ExoSocialActivity existingActivity,
                                      ExoSocialActivity baseComment) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderComments(ExoSocialActivity existingActivity,
                                                  ExoSocialActivity baseComment,
                                                  int limit) {
    return null;
  }

	@Override
	public SortedSet<ActivityProcessor> getActivityProcessors() {
		return null;
	}

	@Override
	public void updateActivity(ExoSocialActivity existingActivity)
			throws ActivityStorageException {
	  //
    DBCollection collection = getCollection(ACTIVITY_COLLECTION_NAME);
    
    BasicDBObject update = new BasicDBObject();
    
    BasicDBObject set = new BasicDBObject();
    //
    fillActivityEntityFromActivity(null, existingActivity, set, false);
    update.append("$set", set);
    
    BasicDBObject query = new BasicDBObject();
    query.put(ActivityMongoEntity.id.getName(), new ObjectId(existingActivity.getId()));
    
    WriteResult result = collection.update(query, update);
    LOG.warn("==============>UPDATED ACTIVITY: "+result.toString());
    
    //
    long currentMillis = System.currentTimeMillis();
    long oldTime = existingActivity.getPostedTime();
    LOG.warn("==============>UPDATED ACTIVITY REF [TIME]: "+ currentMillis);
    updateActivityRef(existingActivity.getId(), currentMillis, oldTime);
    
	}

	@Override
	public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity,
			Long sinceTime) {
		return 0;
	}

	@Override
	public int getNumberOfNewerOnUserActivities(Identity ownerIdentity,
			Long sinceTime) {
		return 0;
	}

	@Override
	public int getNumberOfNewerOnActivitiesOfConnections(
			Identity ownerIdentity, Long sinceTime) {
		return 0;
	}

	@Override
	public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity,
			Long sinceTime) {
		return 0;
	}

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentities(ActivityBuilderWhere where,
                                                           ActivityFilter filter,
                                                           long offset,
                                                           long limit) throws ActivityStorageException {
    return null;
  }

  @Override
  public int getNumberOfSpaceActivities(Identity spaceIdentity) {
    return 0;
  }

  @Override
  public int getNumberOfSpaceActivitiesForUpgrade(Identity spaceIdentity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getSpaceActivities(Identity spaceIdentity, int index, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getSpaceActivitiesForUpgrade(Identity spaceIdentity,
                                                              int index,
                                                              int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity,
                                                       int offset,
                                                       int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity,
                                                       int offset,
                                                       int limit,
                                                       String... activityTypes) {
    return null;
  }

  @Override
  public int getNumberOfActivitiesByPoster(Identity posterIdentity) {
    return 0;
  }

  @Override
  public int getNumberOfActivitiesByPoster(Identity ownerIdentity, Identity viewerIdentity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerOnSpaceActivities(Identity spaceIdentity,
                                                           ExoSocialActivity baseActivity,
                                                           int limit) {
    return null;
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity,
                                               ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getOlderOnSpaceActivities(Identity spaceIdentity,
                                                           ExoSocialActivity baseActivity,
                                                           int limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(Identity spaceIdentity,
                                               ExoSocialActivity baseActivity) {
    return 0;
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnActivityFeed(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnUserActivities(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnActivitiesOfConnections(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnUserSpacesActivities(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfUpdatedOnSpaceActivities(Identity owner, ActivityUpdateFilter filter) {
    return 0;
  }

  @Override
  public int getNumberOfMultiUpdated(Identity owner, Map<String, Long> sinceTimes) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerFeedActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerUserActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerUserSpacesActivities(Identity owner,
                                                              Long sinceTime,
                                                              int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerActivitiesOfConnections(Identity owner,
                                                                 Long sinceTime,
                                                                 int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getNewerSpaceActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderFeedActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderUserActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderUserSpacesActivities(Identity owner,
                                                              Long sinceTime,
                                                              int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderActivitiesOfConnections(Identity owner,
                                                                 Long sinceTime,
                                                                 int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderSpaceActivities(Identity owner, Long sinceTime, int limit) {
    return null;
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(Identity ownerIdentity, Long sinceTime) {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getNewerComments(ExoSocialActivity existingActivity,
                                                  Long sinceTime,
                                                  int limit) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getOlderComments(ExoSocialActivity existingActivity,
                                                  Long sinceTime,
                                                  int limit) {
    return null;
  }

  @Override
  public int getNumberOfNewerComments(ExoSocialActivity existingActivity, Long sinceTime) {
    return 0;
  }

  @Override
  public int getNumberOfOlderComments(ExoSocialActivity existingActivity, Long sinceTime) {
    return 0;
  }

}
