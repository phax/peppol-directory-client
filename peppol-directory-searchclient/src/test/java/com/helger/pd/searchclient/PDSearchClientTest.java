/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.pd.searchclient;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.state.EContinue;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.pd.searchapi.v1.ResultListType;

/**
 * Test class for class {@link PDSearchClient}.
 *
 * @author Philip Helger
 */
public final class PDSearchClientTest
{
  /**
   * Search client that does not issue HTTP calls, but answers every query with a result list
   * matching a fixed total number of results.
   */
  private static final class MockPDSearchClient extends PDSearchClient
  {
    private final int m_nTotalResultCount;
    private final ICommonsList <Integer> m_aRequestedPageIndices = new CommonsArrayList <> ();

    MockPDSearchClient (final int nTotalResultCount)
    {
      super ("http://localhost/");
      m_nTotalResultCount = nTotalResultCount;
    }

    @Override
    @NonNull
    public ResultListType search (@NonNull final PDSearchQuery aQuery) throws IOException
    {
      m_aRequestedPageIndices.add (Integer.valueOf (aQuery.getResultPageIndex ()));

      final int nFirstResultIndex = aQuery.getFirstResultIndex ();
      final ResultListType ret = new ResultListType ();
      ret.setTotalResultCount (m_nTotalResultCount);
      ret.setUsedResultCount (Math.max (0,
                                        Math.min (aQuery.getResultPageCount (),
                                                  m_nTotalResultCount - nFirstResultIndex)));
      ret.setResultPageIndex (aQuery.getResultPageIndex ());
      ret.setResultPageCount (aQuery.getResultPageCount ());
      ret.setFirstResultIndex (nFirstResultIndex);
      ret.setLastResultIndex (nFirstResultIndex + aQuery.getResultPageCount () - 1);
      return ret;
    }
  }

  @Test
  public void testSearchAllPages () throws IOException
  {
    final PDSearchQuery aQuery = PDSearchQuery.createGeneric ("Helger").setResultPageCount (10);

    // No result at all
    try (final MockPDSearchClient aClient = new MockPDSearchClient (0))
    {
      assertEquals (1, aClient.searchAllPages (aQuery, x -> EContinue.CONTINUE));
      assertEquals (new CommonsArrayList <> (Integer.valueOf (0)), aClient.m_aRequestedPageIndices);
    }

    // Exactly one full page
    try (final MockPDSearchClient aClient = new MockPDSearchClient (10))
    {
      assertEquals (1, aClient.searchAllPages (aQuery, x -> EContinue.CONTINUE));
    }

    // Last page is partially filled
    try (final MockPDSearchClient aClient = new MockPDSearchClient (25))
    {
      final ICommonsList <Integer> aUsed = new CommonsArrayList <> ();
      assertEquals (3, aClient.searchAllPages (aQuery, x -> {
        aUsed.add (Integer.valueOf (x.getUsedResultCount ()));
        return EContinue.CONTINUE;
      }));
      assertEquals (new CommonsArrayList <> (Integer.valueOf (10), Integer.valueOf (10), Integer.valueOf (5)), aUsed);
    }

    // Handler stops early
    try (final MockPDSearchClient aClient = new MockPDSearchClient (25))
    {
      assertEquals (2, aClient.searchAllPages (aQuery, x -> EContinue.valueOf (x.getResultPageIndex () != 1)));
    }

    // Start at a later page - the provided query is not modified
    try (final MockPDSearchClient aClient = new MockPDSearchClient (25))
    {
      final PDSearchQuery aLaterQuery = aQuery.getClone ().setResultPageIndex (1);
      assertEquals (2, aClient.searchAllPages (aLaterQuery, x -> EContinue.CONTINUE));
      assertEquals (1, aLaterQuery.getResultPageIndex ());
      assertEquals (new CommonsArrayList <> (Integer.valueOf (1), Integer.valueOf (2)), aClient.m_aRequestedPageIndices);
    }

    // More matches than the server delivers - stops at the maximum result index
    try (final MockPDSearchClient aClient = new MockPDSearchClient (5_000))
    {
      // Results 0-999 in pages of 10
      assertEquals (100, aClient.searchAllPages (aQuery, x -> EContinue.CONTINUE));
    }
  }
}
