/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.storage;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.mongodb.ActivityMongoStorageImpl;
import org.exoplatform.social.core.test.AbstractCoreTest;

public class ActivityMongoStorageImplTestCase extends AbstractCoreTest {
  
  private final Log LOG = ExoLogger.getLogger(ActivityMongoStorageImplTestCase.class);
  
  private IdentityStorage identityStorage;
  private ActivityStorage activityStorage;
  
  private ActivityMongoStorageImpl mongoStorage;
  
  
  private IdentityManager identityManager;
  private RelationshipManager relationshipManager;
  private List<ExoSocialActivity> tearDownActivityList;

  private Identity rootIdentity;
  private Identity johnIdentity;
  private Identity maryIdentity;
  private Identity demoIdentity;
 
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    identityStorage = (IdentityStorage) getContainer().getComponentInstanceOfType(IdentityStorage.class);
    activityStorage = (ActivityStorage) getContainer().getComponentInstanceOfType(ActivityStorage.class);
    mongoStorage = (ActivityMongoStorageImpl) getContainer().getComponentInstanceOfType(ActivityMongoStorageImpl.class);
    identityManager = (IdentityManager) getContainer().getComponentInstanceOfType(IdentityManager.class);
    relationshipManager = (RelationshipManager) getContainer().getComponentInstanceOfType(RelationshipManager.class);
    assertNotNull("identityManager must not be null", identityStorage);
    assertNotNull("activityStorage must not be null", activityStorage);
    rootIdentity = new Identity(OrganizationIdentityProvider.NAME, "root");
    johnIdentity = new Identity(OrganizationIdentityProvider.NAME, "john");
    maryIdentity = new Identity(OrganizationIdentityProvider.NAME, "mary");
    demoIdentity = new Identity(OrganizationIdentityProvider.NAME, "demo");
    
    identityStorage.saveIdentity(rootIdentity);
    identityStorage.saveIdentity(johnIdentity);
    identityStorage.saveIdentity(maryIdentity);
    identityStorage.saveIdentity(demoIdentity);

    assertNotNull("rootIdentity.getId() must not be null", rootIdentity.getId());
    assertNotNull("johnIdentity.getId() must not be null", johnIdentity.getId());
    assertNotNull("maryIdentity.getId() must not be null", maryIdentity.getId());
    assertNotNull("demoIdentity.getId() must not be null", demoIdentity.getId());

    tearDownActivityList = new ArrayList<ExoSocialActivity>();
  }

  @Override
  protected void tearDown() throws Exception {
    for (ExoSocialActivity activity : tearDownActivityList) {
      mongoStorage.deleteActivity(activity.getId());
    }
    identityStorage.deleteIdentity(rootIdentity);
    identityStorage.deleteIdentity(johnIdentity);
    identityStorage.deleteIdentity(maryIdentity);
    identityStorage.deleteIdentity(demoIdentity);
    super.tearDown();
  }
  
  public void testSaveActivity() {
    
    ExoSocialActivity activity = createActivity(0);
    //
    mongoStorage.saveActivity(demoIdentity, activity);
    
    assertNotNull(activity.getId());
    
    ExoSocialActivity rs = mongoStorage.getActivity(activity.getId());
    
    //
    assertEquals("demo", rs.getLikeIdentityIds()[0]);
    assertEquals("john", rs.getMentionedIds()[1]);
    
    //
    tearDownActivityList.add(activity);
    
  }
  
  public void testUpdateActivity() {
    ExoSocialActivity activity = createActivity(1);
    //
    mongoStorage.saveActivity(demoIdentity, activity);
    
    activity.setTitle("Title after updated");
    
    //update
    mongoStorage.updateActivity(activity);
    
    ExoSocialActivity res = mongoStorage.getActivity(activity.getId());
    
    assertEquals("Title after updated", res.getTitle());
    //
    tearDownActivityList.add(activity);
  }
  
  public void testGetActivity() {
    ExoSocialActivity activity = createActivity(1);
    ExoSocialActivity activity2 = createActivity(2);
    //
    mongoStorage.saveActivity(demoIdentity, activity);
    
    
  }
  
  private ExoSocialActivity createActivity(int num) {
    //
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("Activity "+ num);
    activity.setTitleId("TitleID: "+ activity.getTitle());
    activity.setType("UserActivity");
    activity.setBody("Body of "+ activity.getTitle());
    activity.setBodyId("BodyId of "+ activity.getTitle());
    activity.setLikeIdentityIds(new String[]{"demo", "mary"});
    activity.setMentionedIds(new String[]{"demo", "john"});
    activity.setAppId("AppID");
    activity.setExternalId("External ID");
    
    return activity;
  }
  

}
