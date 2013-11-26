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
package org.exoplatform.social.core.mongo.entity;

import org.exoplatform.social.core.storage.query.PropertyLiteralExpression;

public class StreamItemMongoEntity {

  public static final PropertyLiteralExpression<String> id = new PropertyLiteralExpression<String>(String.class, "_id");
  public static final PropertyLiteralExpression<String> activityId = new PropertyLiteralExpression<String>(String.class, "title");
  public static final PropertyLiteralExpression<String> owner = new PropertyLiteralExpression<String>(String.class, "title");
  public static final PropertyLiteralExpression<String> poster = new PropertyLiteralExpression<String>(String.class, "title");
  //LIKER, COMMENTER or MENTIONER
  public static final PropertyLiteralExpression<String> viewerId = new PropertyLiteralExpression<String>(String.class, "title");
  //CONNECTION, LIKER, MENTIONER, COMMENTER
  public static final PropertyLiteralExpression<String[]> viewerTypes = new PropertyLiteralExpression<String[]>(String[].class, "likers");
  public static final PropertyLiteralExpression<Boolean> hiable = new PropertyLiteralExpression<Boolean>(Boolean.class, "title");
  public static final PropertyLiteralExpression<Boolean> lockable = new PropertyLiteralExpression<Boolean>(Boolean.class, "title");
  public static final PropertyLiteralExpression<Long> time = new PropertyLiteralExpression<Long>(Long.class, "title");
  
}
