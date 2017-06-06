### ala-cas-client   [![Build Status](https://travis-ci.org/AtlasOfLivingAustralia/ala-cas-client.svg?branch=master)](https://travis-ci.org/AtlasOfLivingAustralia/ala-cas-client)

---

**NOTE:** Versions after the CAS client 3.4.1 upgrade no longer provide a default value for the roleAttribute CAS property.  If you're using the ALA Auth Grails plugin no action is necessary as the default roleAttribute of 'authority' is provided by the plugin.  Otherwise, you should include a roleAttribute property in the same way you provide other properties to the `AlaHttpServletRequestWrapperFilter`.
