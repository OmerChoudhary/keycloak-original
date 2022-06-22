package org.keycloak.crypto.fips.test;

import java.security.SecureRandom;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.crypto.CryptoProviderTypes;
import org.keycloak.crypto.fips.FIPSAesKeyWrapAlgorithmProvider;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402UnitTest {


    @Test
    public void testFips() throws Exception {
        JWEAlgorithmProvider jweAlg = CryptoIntegration.getProvider().getCryptoUtility(JWEAlgorithmProvider.class, CryptoProviderTypes.AES_KEY_WRAP_ALGORITHM_PROVIDER);
        Assert.assertEquals(jweAlg.getClass(), FIPSAesKeyWrapAlgorithmProvider.class);
    }
}
