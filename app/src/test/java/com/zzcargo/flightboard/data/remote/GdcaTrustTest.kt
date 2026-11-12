package com.zzcargo.flightboard.data.remote

import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import org.junit.Assert.assertThrows
import org.junit.Test

class GdcaTrustTest {

    private val leaf: X509Certificate by lazy {
        javaClass.getResourceAsStream("/certs/zzcargo_leaf.pem")!!.use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }
    }

    private object RejectingTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            throw CertificateException("reject")
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            throw CertificateException("reject")
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    private fun pinnedTrustManager(): PinnedChainTrustManager = PinnedChainTrustManager(
        system = RejectingTrustManager,
        intermediate = GdcaTrust.intermediateCertificate,
        root = GdcaTrust.rootCertificate
    )

    @Test
    fun `accepts the real zzcargo leaf through the pinned chain`() {
        pinnedTrustManager().checkServerTrusted(arrayOf(leaf), "RSA")
    }

    @Test
    fun `production trust manager accepts the real leaf`() {
        GdcaTrust.trustManager().checkServerTrusted(arrayOf(leaf), "RSA")
    }

    @Test
    fun `rejects a certificate that is not the pinned leaf issuer`() {
        assertThrows(CertificateException::class.java) {
            pinnedTrustManager().checkServerTrusted(arrayOf(GdcaTrust.rootCertificate), "RSA")
        }
    }
}
