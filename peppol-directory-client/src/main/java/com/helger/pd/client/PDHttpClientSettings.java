/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.client;

import java.security.GeneralSecurityException;
import java.security.KeyStore.PrivateKeyEntry;

import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.core5.ssl.PrivateKeyStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.HttpClientSettingsConfig;
import com.helger.httpclient.HttpClientSettingsConfig.HttpClientConfig;
import com.helger.httpclient.HttpProxySettings;
import com.helger.httpclient.security.PrivateKeyStrategyFromAliasCaseInsensitive;
import com.helger.httpclient.security.TrustStrategyTrustAll;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;
import com.helger.url.protocol.EURLProtocol;

/**
 * Special {@link HttpClientSettings} that incorporates all the parameters from the configuration.
 *
 * @author Philip Helger
 */
public class PDHttpClientSettings extends HttpClientSettings
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDHttpClientSettings.class);

  /**
   * Constructor.
   *
   * @param sTargetURI
   *        The target URI of the Directory use. May neither be <code>null</code> nor empty.
   * @throws IllegalStateException
   *         If the "https" protocol is used, and the SSL setup is incomplete.
   */
  public PDHttpClientSettings (@NonNull @Nonempty final String sTargetURI)
  {
    resetToConfiguration (sTargetURI);
  }

  /**
   * Apply the deprecated PD client proxy credential properties, in case the standardized ones are
   * not present.
   *
   * @param aProxySettings
   *        The proxy settings to be filled. May not be <code>null</code>.
   */
  @Deprecated (forRemoval = true, since = "1.0.0")
  @SuppressWarnings ("removal")
  private static void _setLegacyProxyCredentials (@NonNull final HttpProxySettings aProxySettings)
  {
    final String sProxyUsername = PDClientConfiguration.getProxyUsername ();
    if (StringHelper.isNotEmpty (sProxyUsername))
    {
      LOGGER.warn ("The support for the configuration properties 'proxy.username' and 'proxy.password' is deprecated. Use 'http.proxy.username' and 'http.proxy.password' instead.");
      aProxySettings.setProxyCredentials (new UsernamePasswordCredentials (sProxyUsername,
                                                                           PDClientConfiguration.getProxyPassword ()));
    }
  }

  /**
   * Overwrite all settings that can appear in the configuration.
   *
   * @param sTargetURI
   *        The target URI to connect to. Makes a difference if this is "http" or "https". May
   *        neither be <code>null</code> nor empty.
   */
  public final void resetToConfiguration (@NonNull @Nonempty final String sTargetURI)
  {
    ValueEnforcer.notEmpty (sTargetURI, "TargetURI");
    final boolean bUseHttps = EURLProtocol.HTTPS.isUsedInURL (sTargetURI);

    // Proxy - reset first, because this method may be called more than once
    final HttpProxySettings aProxySettings = getGeneralProxy ();
    aProxySettings.setProxyHost (null);
    aProxySettings.setProxyCredentials (null);
    aProxySettings.nonProxyHosts ().clear ();

    // Evaluate the standardized "http.proxy.*" configuration properties. The prefix "pdclient." is
    // checked first, so that a PD client specific proxy can be configured, before the unprefixed
    // properties that apply to all components are used
    final HttpClientConfig aHCC = HttpClientConfig.create (PDClientConfiguration.getConfig (), "pdclient", "");
    if (aHCC != null)
      HttpClientSettingsConfig.assignConfigValuesForProxy (aProxySettings, aHCC);

    if (aProxySettings.getProxyCredentials () == null)
      _setLegacyProxyCredentials (aProxySettings);

    if (aProxySettings.getProxyHost () != null)
      LOGGER.info ("PD client uses proxy host " + aProxySettings.getProxyHost ());

    // Reset SSL stuff
    setHostnameVerifier (null);
    setSSLContext (null);

    if (bUseHttps)
    {
      if (PDClientConfiguration.isHttpsHostnameVerificationDisabled ())
      {
        LOGGER.info ("PD client uses disabled hostname verification");
        setHostnameVerifierVerifyAll ();
      }

      // Load key store
      final LoadedKeyStore aLoadedKeyStore = PDClientConfiguration.loadKeyStore ();
      if (aLoadedKeyStore.isFailure ())
      {
        LOGGER.error ("PD client failed to initialize keystore for service connection - can only use http now! Details: " +
                      LoadedKeyStore.getLoadError (aLoadedKeyStore));
      }
      else
      {
        LOGGER.info ("PD client keystore successfully loaded");

        // Sanity check if key can be loaded
        {
          final LoadedKey <PrivateKeyEntry> aLoadedKey = PDClientConfiguration.loadPrivateKey (aLoadedKeyStore.getKeyStore ());
          if (aLoadedKey.isFailure ())
          {
            LOGGER.error ("PD client failed to initialize key from keystore. Details: " +
                          LoadedKey.getLoadError (aLoadedKey));
          }
          else
            LOGGER.info ("PD client key successfully loaded");
        }

        // Load trust store (may not be present/configured)
        final LoadedKeyStore aLoadedTrustStore = PDClientConfiguration.loadTrustStore ();
        if (aLoadedTrustStore.isFailure ())
          LOGGER.error ("PD client failed to initialize truststore for service connection. Details: " +
                        LoadedKeyStore.getLoadError (aLoadedTrustStore));
        else
          LOGGER.info ("PD client truststore successfully loaded");

        try
        {
          final PrivateKeyStrategy aPKS = new PrivateKeyStrategyFromAliasCaseInsensitive (PDClientConfiguration.getKeyStoreKeyAlias ());
          final TrustStrategy aTS = new TrustStrategyTrustAll ();
          setSSLContext (SSLContexts.custom ()
                                    .loadKeyMaterial (aLoadedKeyStore.getKeyStore (),
                                                      PDClientConfiguration.getKeyStoreKeyPassword (),
                                                      aPKS)
                                    .loadTrustMaterial (aLoadedTrustStore.getKeyStore (), aTS)
                                    .build ());
          LOGGER.info ("PD client successfully set SSL context");
        }
        catch (final GeneralSecurityException ex)
        {
          throw new IllegalStateException ("PD client failed to set SSL context", ex);
        }
      }
    }

    // Timeouts
    setConnectTimeout (PDClientConfiguration.getConnectTimeout ());
    setResponseTimeout (PDClientConfiguration.getResponseTimeout ());
  }
}
