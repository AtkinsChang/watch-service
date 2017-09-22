resolvers ++= Resolver.mavenLocal +: DefaultOptions.resolvers(snapshot = false)

libraryDependencies += "org.ow2.asm" % "asm" % "6.0_BETA"
