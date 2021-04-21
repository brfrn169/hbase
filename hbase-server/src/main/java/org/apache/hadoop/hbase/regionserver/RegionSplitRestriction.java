/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.regionserver;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.yetus.audience.InterfaceAudience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A split restriction that restricts the pattern of the split point.
 *
 * The difference between {@link RegionSplitPolicy} and RegionSplitRestriction is that
 * RegionSplitRestriction defines how to split while {@link RegionSplitPolicy} defines when we need
 * to split.
 *
 * There are three implementations as follows:
 * @see NoRegionSplitRestriction
 * @see KeyPrefixRegionSplitRestriction
 * @see DelimitedKeyPrefixRegionSplitRestriction
 */
@InterfaceAudience.Private
public abstract class RegionSplitRestriction {
  private static final Logger LOG = LoggerFactory.getLogger(RegionSplitRestriction.class);

  public static final String RESTRICTION_TYPE_KEY =
    "hbase.regionserver.region.split_restriction.type";

  public static final String RESTRICTION_TYPE_NONE = "None";
  public static final String RESTRICTION_TYPE_KEY_PREFIX = "KeyPrefix";
  public static final String RESTRICTION_TYPE_DELIMITED_KEY_PREFIX = "DelimitedKeyPrefix";

  /**
   * Create the RegionSplitRestriction configured for the given table.
   *
   * @param tableDescriptor the table descriptor
   * @param conf the configuration
   * @return a RegionSplitRestriction instance
   * @throws IOException if an error occurs
   */
  public static RegionSplitRestriction create(TableDescriptor tableDescriptor,
    Configuration conf) throws IOException {
    String type = tableDescriptor.getValue(RESTRICTION_TYPE_KEY);
    if (type == null) {
      type = conf.get(RESTRICTION_TYPE_KEY, RESTRICTION_TYPE_NONE);
    }

    RegionSplitRestriction ret;
    switch (type) {
      case RESTRICTION_TYPE_NONE:
        ret = new NoRegionSplitRestriction();
        break;
      case RESTRICTION_TYPE_KEY_PREFIX:
        ret = new KeyPrefixRegionSplitRestriction();
        break;
      case RESTRICTION_TYPE_DELIMITED_KEY_PREFIX:
        ret = new DelimitedKeyPrefixRegionSplitRestriction();
        break;
      default:
        LOG.warn("Invalid RegionSplitRestriction type specified: {}. "
          + "Using the default RegionSplitRestriction", type);
        ret = new NoRegionSplitRestriction();
        break;
    }
    ret.initialize(tableDescriptor, conf);
    return ret;
  }

  /**
   * Initialize the RegionSplitRestriction instance
   *
   * @param tableDescriptor the table descriptor
   * @param conf the configuration
   * @throws IOException if an error occurs
   */
  public abstract void initialize(TableDescriptor tableDescriptor, Configuration conf)
    throws IOException;

  /**
   * Returns a restricted split point.
   *
   * @param splitPoint the split point determined by {@link RegionSplitPolicy} or specified by a
   *   user manually
   * @return the restricted split point
   */
  public abstract byte[] getRestrictedSplitPoint(byte[] splitPoint);
}
