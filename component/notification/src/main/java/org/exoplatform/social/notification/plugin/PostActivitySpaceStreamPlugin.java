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
package org.exoplatform.social.notification.plugin;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.exoplatform.commons.api.notification.MessageInfo;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessage;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.SocialMessageBuilder;
import org.exoplatform.social.notification.Utils;

public class PostActivitySpaceStreamPlugin extends AbstractNotificationPlugin {
  private final String ID = "ActivityPostSpaceProvider";
  
  @Override
  public String getId() {
    return ID;
  }

  @Override
  public NotificationMessage makeNotification(NotificationContext ctx) {
    try {
      ExoSocialActivity activity = ctx.value(SocialMessageBuilder.ACTIVITY);
      Space space = Utils.getSpaceService().getSpaceByPrettyName(activity.getStreamOwner());
      
      
      return NotificationMessage.instance()
                                .key(getId())
                                .with(SocialNotificationUtils.POSTER.getKey(), Utils.getUserId(activity.getPosterId()))
                                .with(SocialNotificationUtils.ACTIVITY_ID.getKey(), activity.getId())
                                .to(Utils.getDestinataires(activity, space)).end();
    } catch (Exception e) {
      ctx.setException(e);
    }
    
    return null;
  }

  @Override
  public MessageInfo makeMessage(NotificationContext ctx) {
    MessageInfo messageInfo = new MessageInfo();
    Map<String, String> templateContext = new HashMap<String, String>();
    
    NotificationMessage notification = ctx.getNotificationMessage();
    
    String language = getLanguage(notification);
    
    String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
    ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
    Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
    
    Identity spaceIdentity = Utils.getIdentityManager().getOrCreateIdentity(SpaceIdentityProvider.NAME, activity.getStreamOwner(), true);
    
    templateContext.put("USER", identity.getProfile().getFullName());
    templateContext.put("SPACE", spaceIdentity.getProfile().getFullName());
    String subject = Utils.getTemplateGenerator().processSubjectIntoString(notification.getKey().getId(), templateContext, language);
    
    templateContext.put("ACTIVITY", activity.getTitle());
    templateContext.put("REPLY_ACTION_URL", LinkProviderUtils.getReplyActivityUrl(activity.getId(), notification.getSendToUserIds().get(0)));
    templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getViewFullDiscussionUrl(activity.getId(), notification.getSendToUserIds().get(0)));
    String body = Utils.getTemplateGenerator().processTemplate(notification.getKey().getId(), templateContext, language);
    
    return messageInfo.subject(subject).body(body).end();
  }

  @Override
  public boolean makeDigest(NotificationContext ctx, Writer writer) {
    
    return true;
  }

}