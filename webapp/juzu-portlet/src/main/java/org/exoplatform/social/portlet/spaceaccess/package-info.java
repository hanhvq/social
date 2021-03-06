@Application 
@Portlet 
@Assets(
  scripts = {
    @Script(id = "jquery", src = "js/jquery-1.7.1.min.js"),
    @Script(src = "js/less-1.2.2.min.js", depends = "jquery"),
    @Script(src = "js/bootstrap.js", depends = "jquery"),
    @Script(src = "js/bootstrap-collapse.js", depends = "jquery"),
    @Script(src = "js/bootstrap-tooltip.js", depends = "jquery"),
    @Script(src = "js/bootstrap-popover.js", depends = "jquery"),
    @Script(src = "js/space-access.js", depends = "jquery")
  },
  stylesheets = {
    @Stylesheet(src = "css/gatein.less"),
    @Stylesheet(src = "css/space-access/space-access.less")
  },
  location = juzu.asset.AssetLocation.SERVER
)
package org.exoplatform.social.portlet.spaceaccess;

import juzu.Application;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.portlet.Portlet;