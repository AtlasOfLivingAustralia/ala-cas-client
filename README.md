### ala-cas-client   [![Build Status](https://travis-ci.org/AtlasOfLivingAustralia/ala-cas-client.svg?branch=master)](https://travis-ci.org/AtlasOfLivingAustralia/ala-cas-client)

---

**NOTE:** Version 2.3+ no longer provides a default value for the `roleAttribute` or `ignoreCase` CAS properties.  If you're using the ALA Auth Grails plugin no action is necessary as defaults of 'authority' for roleAttribute and `true` for 'ignoreCase' are provided by the plugin.  Otherwise, you should include roleAttribute and ignoreCase properties in the same way you provide other properties to the `AlaHttpServletRequestWrapperFilter`.
