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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.yetus.audience.InterfaceAudience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Arrays;

/**
 * A RegionSplitPointRestriction implementation that groups rows by a prefix of the row-key with a
 * delimiter. Only the first delimiter for the row key will define the prefix of the row key that
 * is used for grouping.
 *
 * This ensures that a region is not split "inside" a prefix of a row key.
 * I.e. rows can be co-located in a region by their prefix.
 *
 * As an example, if you have row keys delimited with <code>_</code>, like
 * <code>userid_eventtype_eventid</code>, and use prefix delimiter _, this split policy ensures
 * that all rows starting with the same userid, belongs to the same region.
 */
@InterfaceAudience.Private
public class DelimitedKeyPrefixRegionSplitPointRestriction extends RegionSplitPointRestriction {
  private static final Logger LOGGER =
    LoggerFactory.getLogger(DelimitedKeyPrefixRegionSplitPointRestriction.class);

  public static final String DELIMITER_KEY =
    "hbase.regionserver.region.split_point_restriction.delimiter";

  private byte[] delimiter = null;

  @Override
  public void initialize(TableDescriptor tableDescriptor, Configuration conf) throws IOException {
    String delimiterString = tableDescriptor.getValue(DELIMITER_KEY);
    if (delimiterString == null || delimiterString.length() == 0) {
      delimiterString = conf.get(DELIMITER_KEY);
      if (delimiterString == null || delimiterString.length() == 0) {
        LOGGER.error("{} not specified for table {}. "
          + "Using the default RegionSplitPointRestriction", DELIMITER_KEY,
          tableDescriptor.getTableName());
        return;
      }
    }
    delimiter = Bytes.toBytes(delimiterString);
  }

  @Override
  public byte[] getRestrictedSplitPoint(byte[] splitPoint) {
    if (delimiter != null) {
      // find the first occurrence of delimiter in split point
      int index = org.apache.hbase.thirdparty.com.google.common.primitives.Bytes.indexOf(
        splitPoint, delimiter);
      if (index < 0) {
        LOGGER.warn("Delimiter {} not found for split key {}", Bytes.toString(delimiter),
          Bytes.toStringBinary(splitPoint));
        return splitPoint;
      }

      // group split keys by a prefix
      return Arrays.copyOf(splitPoint, Math.min(index, splitPoint.length));
    } else {
      return splitPoint;
    }
  }
}
