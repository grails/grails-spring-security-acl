includeTargets << new File("$springSecurityAclPluginDir/scripts/S2CreateAclDomains.groovy")

ant.echo """
*****************************************************
* You've installed the Spring Security ACL plugin.  *
*****************************************************
"""

s2CreateAclDomains()
