package org.exoplatform.social.core.storage.mongodb;

import org.exoplatform.social.core.storage.impl.AbstractStorage;

import com.mongodb.DBCollection;

public abstract class AbstractMongoStorage extends AbstractStorage {
  
  /** . */
  private MongoStorage storage;
  
  public AbstractMongoStorage(MongoStorage storage) {
    this.storage = storage;
  }
  
  /**
   * Gets the collection specified by name
   * 
   * @param name the name of collection.
   * @return
   */
  protected DBCollection getCollection(String name) {
    return storage.getDB().getCollection(name);
  }
  
  /**
   * Gets the mongo storage
   * @return
   */
  public MongoStorage getMongoStorage() {
    return this.storage;
  }

}
