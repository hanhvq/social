/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

@Portlet
@Application(name = "ActionProfile")
@Bindings({ 
    @Binding(PluginSettingService.class), 
    @Binding(UserSettingService.class) 
  }
)

package org.exoplatform.social.portlet.actionProfile;

import juzu.Application;
import juzu.plugin.portlet.Portlet;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
