### ala-cas-client   [![Build Status](https://travis-ci.org/AtlasOfLivingAustralia/ala-cas-client.svg?branch=master)](https://travis-ci.org/AtlasOfLivingAustralia/ala-cas-client)

The latest version is: `4.0.0 - SNAPSHOT`, which supports Grails 7.1.1.
NOTES: Grails 7.1.1 is not compatible with Grails 6

If you are using Grails 6 or below, `3.1.0 - SNAPSHOT` is recommended.

---

**NOTE:** Version 2.3+ no longer provides a default value for the `roleAttribute` or `ignoreCase` CAS properties.  If you're using the ALA Auth Grails plugin no action is necessary as defaults of 'role' for roleAttribute and `true` for 'ignoreCase' are provided by the plugin.  Otherwise, you should include roleAttribute and ignoreCase properties in the same way you provide other properties to the `HttpServletRequestWrapperFilter`.
