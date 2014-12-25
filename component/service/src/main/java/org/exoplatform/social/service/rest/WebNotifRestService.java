/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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

package org.exoplatform.social.service.rest;

import static org.exoplatform.social.service.rest.RestChecker.checkAuthenticatedRequest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.commons.api.notification.model.WebNotificationFilter;
import org.exoplatform.commons.api.notification.service.WebNotificationService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * 
 * Provides REST Services for Web Channel.
 * 
 * @anchor NotificationRestService
 */

@Path("social/webNotification")
public class WebNotifRestService implements ResourceContainer {
  private static int DEFAULT_OFFSET = 0;
  private static int DEFAULT_LIMIT = 13; // 20?
  
  private WebNotificationService webNotifServ; 
  
  public WebNotifRestService() {
  }
  
  @GET
  @Path("getContents/{userId}.{format}")
  public Response getNotificationContents(@Context UriInfo uriInfo,
                                          @PathParam("userId") String userId,
                                          @PathParam("format") String format,
                                          @QueryParam("offset") int offset,
                                          @QueryParam("limit") int limit,
                                          @QueryParam("isOnpopOver") boolean isOnpopOver) throws Exception {
    checkAuthenticatedRequest();
    
    if (format.indexOf('.') > 0) {
      userId = new StringBuffer(userId).append(".").append(format.substring(0, format.lastIndexOf('.'))).toString();
      format = format.substring(format.lastIndexOf('.') + 1);
    }
    
    String[] mediaTypes = new String[] { "json", "xml" };
    format = ArrayUtils.contains(mediaTypes, format) ? format : mediaTypes[0];
    
    //
    MediaType mediaType = Util.getMediaType(format, mediaTypes);
  
    WebNotifContent webNotifContent = new WebNotifContent();
    offset = offset != 0 ? offset : DEFAULT_OFFSET;
    limit = limit != 0 ? limit : DEFAULT_LIMIT;
    
    List<String> webNotificationContents = getWebNotificationService()
        .getNotificationContents(new WebNotificationFilter(userId, isOnpopOver), offset, limit);
    for (String webNotificationContent : webNotificationContents) {
      webNotifContent.setContent(webNotificationContent);
    }
    return Util.getResponse(webNotifContent, uriInfo, mediaType, Response.Status.OK);
  }
  
  private WebNotificationService getWebNotificationService() {
    PortalContainer portalContainer = PortalContainer.getInstance();
    if (webNotifServ == null) {
      webNotifServ = (WebNotificationService) portalContainer
          .getComponentInstanceOfType(WebNotificationService.class);
    }
    
    return webNotifServ;
  }
  
  @XmlRootElement
  static public class WebNotifContent {
    String content;

    public WebNotifContent() {
    }
    
    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }
  }
}
