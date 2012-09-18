hippo-orphan-user-group-relation-runner
=======================================

Hippo CMS 7 runner that removes user-references in groups to users that don't exist anymore.

HOWTO: User this runner
- Either build the runner from source or download a binary distribution
- Go to the folder where you extracted or built the runner files
- Edit the file "runner.properties":
  - enter the right url, useername and password for the repository connection
  - set the property "plugins.java.orphan-deleter.commit" to the prefered value. "false" means the logger will only
    log it's findings but won't make any changes. "true" means that the logger will delete group memberships for users
    that no longer exist.

HOWTO: Build from source
- run "mvn clean package appassembler:assemble"
- The runner package will be waiting for you under target/jcr-runner