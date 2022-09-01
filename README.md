## platform-qa-clients

### Overview

* The main purpose of the platform-qa-clients library is to provide clients to work with different third-parties;
* ceph-qa-client - implemented to work with ceph in tests;
* database-qa-client implemented to work with database in tests;
* email-qa-client implemented to work with mailing in tests;
* git-qa-client implemented to work with git/gerrit in tests;
* jenkins-qa-client implemented to work with jenkins in tests;
* keycloak-qa-client implemented to work with keycloak in tests;
* openshift-qa-client implemented to work with openshift in tests;
* protocols-qa-client implemented to work with different API protocols in tests;
* vault-qa-client implemented to work with vault service in tests.

### Usage
 Use this clients in platform or registry tests when integration with appropriate 
 third-party is needed.

### Test execution

* Tests could be run via maven command:
    * `mvn verify` OR using appropriate functions of your IDE.

### License

The platform-qa-clients is Open Source software released under
the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).


