/* Copyright (c) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nncloudtv.model;

import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.nncloudtv.lib.NnStringUtil;

/**
 * One shard belonging to the named counter.
 *
 * An individual shard is written to infrequently to allow the counter in
 * aggregate to be incremented rapidly.
 *
 */
@PersistenceCapable(table = "counter_shard", detachable = "true")
public class CounterShard implements PersistentModel {
    
    private static final long serialVersionUID = 7031802928943024767L;
    private static final boolean cachable = false;
    
    public boolean isCachable() {
        return cachable;
    }
    
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private long id;
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    @Persistent
    private int shardNumber;
    
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.NORMAL_STRING_LENGTH)
    private String counterName;
    
    @Persistent
    private long count;
    
    public CounterShard(String counterName, int shardNumber) {
        
        this.counterName = counterName;
        this.shardNumber = shardNumber;
        this.count = 0;
    }
    
    public String getCounterName() {
        return counterName;
    }
    
    public Integer getShardNumber() {
        return shardNumber;
    }
    
    public Long getCount() {
        return count;
    }
    
    public void setCount(Long count) {
        this.count = count;
    }
    
    public void increment(int amount) {
        this.count += amount;
    }
    
    public void setUpdateDate(Date date) {
        // TODO Auto-generated method stub
        
    }
    
    public Date getUpdateDate() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public void setCreateDate(Date date) {
        // TODO Auto-generated method stub
        
    }
    
    public Date getCreateDate() {
        // TODO Auto-generated method stub
        return null;
    }
}
