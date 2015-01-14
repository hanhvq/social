/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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

package org.exoplatform.social.portlet.experienceProfile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.inject.Inject;

import juzu.Path;
import juzu.Response;
import juzu.View;
import juzu.impl.request.Request;
import juzu.template.Template;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.portlet.userprofile.helpers.UserProfileHelper;
import org.exoplatform.social.portlet.userprofile.helpers.UserProfileHelper.Context;
import org.exoplatform.social.webui.Utils;

public class ExperienceProfile {
  private static final Log LOG = ExoLogger.getLogger(ExperienceProfile.class);
  final protected static String EXPERIENCES_IS_CURRENT = Profile.EXPERIENCES_IS_CURRENT;

  @Inject
  @Path("index.gtmpl") Template index;
  
  @Inject
  ResourceBundle bundle; 
  
  @View
  public Response.Content index(Request request) {
    
    Map<String, Object> parameters = new HashMap<String, Object>();
    
    try {
      parameters.put("experiences", getExperience());
    } catch (Exception e) {
      LOG.error("Could not get experiences." + e);
    }
    parameters.put("_ctx", Context.getInstance(request, bundle));
    parameters.put("aboutMe", getAboutMe());
    parameters.put("EXPERIENCES_IS_CURRENT", EXPERIENCES_IS_CURRENT);
    parameters.put("isOwner", Utils.isOwner());

    return index.ok(parameters);
  }
  
  protected String getAboutMe() {
    String about = (String) UserProfileHelper.getCurrentProfile().getProperty(Profile.ABOUT_ME);
    return UserProfileHelper.isEmpty(about) ? "" : about;
  }
  
  protected List<Map<String, String>> getExperience() throws Exception {
    return UserProfileHelper.getDisplayExperience(UserProfileHelper.getCurrentProfile());
  }
}
