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
package org.exoplatform.social.core.mongo.entity;

import org.exoplatform.social.core.storage.query.PropertyLiteralExpression;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 21, 2013  
 */
public class ActivityMongoEntity {

  public static final PropertyLiteralExpression<String> id = new PropertyLiteralExpression<String>(String.class, "_id");
  public static final PropertyLiteralExpression<String> title = new PropertyLiteralExpression<String>(String.class, "title");
  public static final PropertyLiteralExpression<String> titleId = new PropertyLiteralExpression<String>(String.class, "titleId");
  public static final PropertyLiteralExpression<String> body = new PropertyLiteralExpression<String>(String.class, "body");
  public static final PropertyLiteralExpression<String> bodyId = new PropertyLiteralExpression<String>(String.class, "bodyId");
  public static final PropertyLiteralExpression<String[]> likers = new PropertyLiteralExpression<String[]>(String[].class, "likers");
  public static final PropertyLiteralExpression<String[]> mentioners = new PropertyLiteralExpression<String[]>(String[].class, "mentioners");
  public static final PropertyLiteralExpression<String> appId = new PropertyLiteralExpression<String>(String.class, "appId");
  public static final PropertyLiteralExpression<String> externalId = new PropertyLiteralExpression<String>(String.class, "externalId");
  
}
