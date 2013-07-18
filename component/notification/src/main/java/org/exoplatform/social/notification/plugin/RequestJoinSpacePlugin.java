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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.exoplatform.commons.api.notification.MessageInfo;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessage;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;

public class RequestJoinSpacePlugin extends AbstractNotificationPlugin {
  public final String ID = "RequestJoinSpace";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public NotificationMessage makeNotification(NotificationContext ctx) {
    Space space = ctx.value(SocialNotificationUtils.SPACE);
    String userId = ctx.value(SocialNotificationUtils.REMOTE_ID);
    
    return NotificationMessage.instance().key(getId())
           .with("request_from", userId)
           .with("spaceId", space.getId())
           .to(Arrays.asList(space.getManagers()));
  }

  @Override
  public MessageInfo makeMessage(NotificationContext ctx) {
    MessageInfo messageInfo = new MessageInfo();
    Map<String, String> templateContext = new HashMap<String, String>();
    
    NotificationMessage notification = ctx.getNotificationMessage();
    
    String language = getLanguage(notification);
    
    String spaceId = notification.getValueOwnerParameter(SocialNotificationUtils.SPACE_ID.getKey());
    Space space = Utils.getSpaceService().getSpaceById(spaceId);
    Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getValueOwnerParameter("request_from"), true);
    Profile userProfile = identity.getProfile();
    
    templateContext.put("SPACE", space.getDisplayName());
    templateContext.put("USER", userProfile.getFullName());
    String subject = Utils.getTemplateGenerator().processSubjectIntoString(notification.getKey().getId(), templateContext, language);
    
    templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(userProfile));
    templateContext.put("VALIDATE_SPACE_REQUEST_ACTION_URL", LinkProviderUtils.getValidateRequestToJoinSpaceUrl(space.getId(), identity.getRemoteId()));
    String body = Utils.getTemplateGenerator().processTemplate(notification.getKey().getId(), templateContext, language);
    
    return messageInfo.subject(subject).body(body).end();
  }

  @Override
  public boolean makeDigest(NotificationContext ctx, Writer writer) {
    return true;
  }

}