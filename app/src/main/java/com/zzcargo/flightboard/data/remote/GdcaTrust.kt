package com.zzcargo.flightboard.data.remote

import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * zzcargo.com's server sends only its leaf certificate, so Android cannot build
 * leaf -> GDCA intermediate -> GDCA root on its own. This keeps normal system
 * validation first and falls back to the pinned GDCA chain.
 */
internal object GdcaTrust {

    private val intermediatePem = """
        -----BEGIN CERTIFICATE-----
        MIIFtDCCA5ygAwIBAgIQI+7i/Ehv5ku35sh2zSQUYTANBgkqhkiG9w0BAQsFADBi
        MQswCQYDVQQGEwJDTjEyMDAGA1UECgwpR1VBTkcgRE9ORyBDRVJUSUZJQ0FURSBB
        VVRIT1JJVFkgQ08uLExURC4xHzAdBgNVBAMMFkdEQ0EgVHJ1c3RBVVRIIFI1IFJP
        T1QwHhcNMjYwMjI1MDU1OTUyWhcNMzYxMjMwMTYwMDAwWjBvMQswCQYDVQQGEwJD
        TjE5MDcGA1UECgwwR2xvYmFsIERpZ2l0YWwgQ3liZXJzZWN1cml0eSBBdXRob3Jp
        dHkgQ28uLCBMdGQuMSUwIwYDVQQDDBxHRENBIFRydXN0QVVUSCBSNCBPViBTU0wg
        Q0EyMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxUvuU+aTBBacAjA7
        vjXetuUbM55aa2kWzqGxnLnhaCkquRe0sKdXGX/xtqYEiSpr4A12074h3DDLCyJw
        kH44AsF8i5phZpgXvmBcioove5XuA/kwxdvhszwVc7eSB59R132aM4NmJWWwrdJ0
        Gl/6mOMTTqGNv07yZvucHF8ouxjs22Ck++HOGajQyDYPTZgyVP3Uh3X2IcszknOd
        RfAfrhEDYgUpCWSSqK0+yqStlAMIFqg2TWcQ7elvpBDU1Sz2PCI3g3ETMH19uJ7e
        iJI2zwO8D8WpP6pHUQMGP0fTgtaOWZa7YNigFUsBfVdlL1Y1pSn1VG7800NcfXUw
        uvDlEwIDAQABo4IBVzCCAVMwfQYIKwYBBQUHAQEEcTBvMEIGCCsGAQUFBzAChjZo
        dHRwOi8vd3d3LmdkY2EuY29tLmNuL2NlcnQvR0RDQV9UcnVzdEFVVEhfUjVfUk9P
        VC5kZXIwKQYIKwYBBQUHMAGGHWh0dHA6Ly9vY3NwMi5nZGNhLmNvbS5jbi9vY3Nw
        MB0GA1UdDgQWBBRaQelEvTVgr+LVrGW4IEJgsbrpiTASBgNVHRMBAf8ECDAGAQH/
        AgEAMB8GA1UdIwQYMBaAFOLJQJ9NzuiaoXzPDj9lxSmIahlRMBEGA1UdIAQKMAgw
        BgYEVR0gADBGBgNVHR8EPzA9MDugOaA3hjVodHRwOi8vY3JsLmdkY2EuY29tLmNu
        L2NybC9HRENBX1RydXN0QVVUSF9SNV9ST09ULmNybDAOBgNVHQ8BAf8EBAMCAQYw
        EwYDVR0lBAwwCgYIKwYBBQUHAwEwDQYJKoZIhvcNAQELBQADggIBACDu5rPV5keg
        +qI/rf23js3T8AaXJfPEhRLJ4J59C4mLSffs3+QNCTqvTjPakARlPnCwWQbZ5vln
        +tswhdD3I7/Di7BSHr+zea9sNSmVKhrdD1ttaCg0roJB8nkkmSQKPaM9htIxK5Dm
        VRhuIxQxLq0k3JCjhcLTmQ1vmRcf8x6mqFQRcc+h9kqmkfj3UWJ5JxVdhVOfU0i/
        0yT3l1shHDDEVGQexdzvZDrATzJ2NtKaGvFnF0IxDpQ8WKGtWwFinx3a7x0zYTLf
        cBQDSsyuVobnX+N8HY7fR+3drb5XGeLH7fSr9q5wv8KxShsISW2O+awZtTIgpYfo
        cxE+R9vTUbH1drZ2laY4nEtYyIWMZlBpoGpeWvnJrZ0QYrA4Ech7YoMbqF6FW9ih
        pdoOEXKgcUnwzn3d3xLL80//+6V+4oia/Li5Q5wmzIkJj+1/0YIxmHF8KHKanEWD
        +hcqyZSoYTyTserIJIc9bgi96d5RhJO4QGgZhVrAN/Ly2ja7I6NtTmCwKUFHER3h
        Ssdx1IMj0Bj0rWB8Hd5nuf8wdGZYjAMA4J1jW7rE2F5t9zPr9eXybDg9IGLT8stK
        Qb7w7zUVWQlKr2zuZwzcUK06+y6LaS/T3ebHG40X4mvxS50ya94t4b6YcF8cDKmn
        WTuExaAcEPHXMKk2704w1l0iJSTgaoUd
        -----END CERTIFICATE-----
    """.trimIndent()

    private val rootPem = """
        -----BEGIN CERTIFICATE-----
        MIIFiDCCA3CgAwIBAgIIfQmX/vBH6nowDQYJKoZIhvcNAQELBQAwYjELMAkGA1UE
        BhMCQ04xMjAwBgNVBAoMKUdVQU5HIERPTkcgQ0VSVElGSUNBVEUgQVVUSE9SSVRZ
        IENPLixMVEQuMR8wHQYDVQQDDBZHRENBIFRydXN0QVVUSCBSNSBST09UMB4XDTE0
        MTEyNjA1MTMxNVoXDTQwMTIzMTE1NTk1OVowYjELMAkGA1UEBhMCQ04xMjAwBgNV
        BAoMKUdVQU5HIERPTkcgQ0VSVElGSUNBVEUgQVVUSE9SSVRZIENPLixMVEQuMR8w
        HQYDVQQDDBZHRENBIFRydXN0QVVUSCBSNSBST09UMIICIjANBgkqhkiG9w0BAQEF
        AAOCAg8AMIICCgKCAgEA2aMW8Mh0dHeb7zMNOwZ+Vfy1YI92hhJCfVZmPoiC7XJj
        Dp6L3TQsAlFRwxn9WVSEyfFrs0yw6ehGXTjGoqcuEVe6ghWinI9tsJlKCvLriXBj
        TnnEt1u9ol2x8kECK62pOqPseQrsXzrj/e+APK00mxqriCZ7VqKChh/rNYmDf1+u
        KU49tm7srsHwJ5uu4/Ts765/94Y9cnrrpftZTqfrlYwiOXnhLQiPzLyRuEH3FMEj
        qcOtmkVEs7LXLM3GKeJQEK5cy4KOFxg2fZfmiJqwTTQJ9Cy5WmYqsBebnh52nUpm
        MUHfP/vFBu8btn4aRjb3ZGM74zkYI+dndRTVdVeSN72+ahsmUPI2JgaQxXABZG12
        ZuGR224HwGGALrIuL4xwp9E7PLOR5G62xDtw8mySlwnNR30YwPO7ng/Wi64HtloP
        zgsMR6flPri9fcebNaBhlzpBdRfMK5Z3KpIhHtmVdiBnaM8Nvd/WHwlqmuLMc3Gk
        L30SgLdTMEZeS1SZD2fJpcjyIMGC7J0R38IC+xo70e0gmu9lZJIQDSri3nDxGGeC
        jGHeuLzRL5z7D9Ar7Rt2ueQ5Vfj4oR24qoAATILnsn8JuLwwoC8N9VKejveSswoA
        HQBUlwbgsQfZxw9cZX08bVlX5O2ljelAU58VS6Bx9hoh49pwBiFYFIeFd3mqgnkC
        AwEAAaNCMEAwHQYDVR0OBBYEFOLJQJ9NzuiaoXzPDj9lxSmIahlRMA8GA1UdEwEB
        /wQFMAMBAf8wDgYDVR0PAQH/BAQDAgGGMA0GCSqGSIb3DQEBCwUAA4ICAQDRSVfg
        p8xoWLoBDysZzY2wYUWsEe1jUGn4H3++Fo/9nesLqjJHdtJnJO29fDMylyrHBYZm
        DRd9FBUb1Ov9H5r2XpdptxolpAqzkT9fNqyL7FeoPueBihhXOYV0GkLH6VsTX4/5
        COmSdI31R9KrO9b7eGZONn356ZLpBN79SWP8bfsUcZNnL0dKt7n/HipzcEYwv1ry
        L3ml4Y0M2fmyYzeMN2WFcGpcWwlyua1jPLHd+PwyvzeG5LuOmCd+uh8W4XAR8gPf
        JWIyJyYYMoSf/wA6E7qaTfRPuBRwIrHKK5DOKcFw9C+df/KQHtZa37dG/OaG+svg
        IHZ6uqbL9XzeYqWxi+7egmaKTjowHz+Ay60nugxe19CxVsp3cbK1daFQqUBDF8Io
        2c9Si1vIY9RCPqAzekYu9wogRlR+ak8x8YF+QnQ4ZXMn7sZ8uI7XpTrXmKGcjBBV
        09tL7ECQ8s1uV9JiDnxXk7Gnbc2dg7sq5+W2O3FYrf3RRbxake5TFW/TRQl1brqQ
        XR4EzzffHqhmsYzmIGrv/EhOdJhCrylvLmrH+33RZjEizIYAfmaDDEL0vTSSwxrq
        T8p+ck0LcIymSLumoRT2+1hEmRSuqguTaaApJUqlyyvdimYHFngVV3Eb7PVHhPOe
        MTd61X8kreS8/f3MboPoDKi3QWwH3b08hpcv0g==
        -----END CERTIFICATE-----
    """.trimIndent()

    val intermediateCertificate: X509Certificate by lazy { parse(intermediatePem) }
    val rootCertificate: X509Certificate by lazy { parse(rootPem) }

    fun trustManager(): X509TrustManager = PinnedChainTrustManager(
        system = defaultSystemTrustManager(),
        intermediate = intermediateCertificate,
        root = rootCertificate
    )

    fun sslContext(manager: X509TrustManager): SSLContext =
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(manager), SecureRandom())
        }

    private fun defaultSystemTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
            ?: throw IllegalStateException("System X509TrustManager is unavailable")
    }

    private fun parse(pem: String): X509Certificate =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(pem.toByteArray(Charsets.US_ASCII)))
            as X509Certificate
}

internal class PinnedChainTrustManager(
    private val system: X509TrustManager,
    private val intermediate: X509Certificate,
    private val root: X509Certificate
) : X509TrustManager {

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("Client authentication is not supported")
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val serverChain = chain ?: throw CertificateException("证书链为空")
        if (serverChain.isEmpty()) throw CertificateException("证书链为空")
        val leaf = serverChain[0]

        var systemFailure: CertificateException? = null
        try {
            system.checkServerTrusted(serverChain, authType)
            return
        } catch (e: CertificateException) {
            systemFailure = e
        }

        if (leaf.issuerX500Principal != intermediate.subjectX500Principal) {
            throw CertificateException("证书签发者不受信任", systemFailure)
        }

        try {
            leaf.checkValidity()
            leaf.verify(intermediate.publicKey)
            intermediate.checkValidity()
            intermediate.verify(root.publicKey)
            root.checkValidity()
            root.verify(root.publicKey)
        } catch (e: Exception) {
            throw CertificateException("GDCA 证书链校验失败", e)
        }

        if (intermediate.basicConstraints < 0 || root.basicConstraints < 0) {
            throw CertificateException("GDCA CA 证书无效", systemFailure)
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> =
        system.acceptedIssuers + arrayOf(intermediate, root)
}
