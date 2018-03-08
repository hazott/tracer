suite = {
  "mxversion" : "5.136.5",
  "name" : "core",
  "versionConflictResolution" : "latest",

  "imports" : {
    "suites" : [
      {
        "name" : "truffle",
        "subdir" : True,
        "version" : "38f0c6f147d862e43c80ebabb9a0a46f628b9750",
        "urls" : [
          {"url" : "https://github.com/oracle/graal", "kind" : "git"},
          {"url" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind" : "binary"},
        ]
      }
    ]
  },

  "javac.lint.overrides" : "none",

  "licenses" : {
    "GPLv3" : {
      "name" : "GNU General Public License, version 3",
      "url" : "https://opensource.org/licenses/GPL-3.0",
    },
    "MPL-1.1" : {
      "name" : "Mozilla Public License 1.1",
      "url" : "https://opensource.org/licenses/MPL-1.1"
    },
    "MPL-2.0" : {
      "name" : "Mozilla Public License 2.0",
      "url" : "https://opensource.org/licenses/MPL-2.0"
    }
  },

  "libraries" : {
    "CORE" : {
      "path" : "lib/core.jar",
      "urls" : [
        "https://orakel.has.enough.coffee/repo/everyware/core.jar"
      ],
      "sha1" : "d9a089a33b64d5470e07591ae71bcf67acaa78f5",
    },

    "POSIX" : {
      "path" : "lib/posix.jar",
      "urls" : [
        "https://orakel.has.enough.coffee/repo/everyware/posix.jar"
      ],
      "sha1" : "f8bf00407a162aba1c98c019c71a4f9daece63a3",
    },
  },

  "defaultLicense" : "GPLv3",

  "projects" : {
    "org.graalvm.vm.memory" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "CORE",
        "POSIX",
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_NFI"
      ],
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "vmx86",
      "license" : "GPLv3",
    },

    "org.graalvm.vm.x86" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "org.graalvm.vm.memory",
        "CORE",
        "POSIX",
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_NFI"
      ],
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
      "workingSets" : "vmx86",
      "license" : "GPLv3",
    },

    "org.graalvm.vm.x86.testcases.asm" : {
      "subDir" : "tests",
      "class" : "VMX86TestSuite",
      "license" : "GPLv3"
    },

    "org.graalvm.vm.x86.testcases.c" : {
      "subDir" : "tests",
      "class" : "VMX86TestSuite",
      "license" : "GPLv3"
    },

    "org.graalvm.vm.x86.test" : {
      "subDir" : "projects",
      "sourceDirs" : ["src"],
      "dependencies" : [
        "org.graalvm.vm.x86",
        "org.graalvm.vm.x86.testcases.asm",
        "org.graalvm.vm.x86.testcases.c",
        "mx:JUNIT",
      ],
      "javaCompliance" : "1.8",
      "workingSets" : "vmx86",
      "license" : "GPLv3",
    }
  },

  "distributions" : {
    "VM" : {
      "path" : "build/vmx86.jar",
      "subDir" : "vmx86",
      "sourcesPath" : "build/vmx86.src.zip",
      "dependencies" : [
        "org.graalvm.vm.x86",
      ],
      "distDependencies" : [
        "CORE",
        "POSIX",
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_NFI"
      ]
    },

    "VM_TESTCASES" : {
      "native" : True,
      "platformDependent" : True,
      "output" : "mxbuild/testcases",
      "dependencies" : [
        "org.graalvm.vm.x86.testcases.asm",
        "org.graalvm.vm.x86.testcases.c"
      ],
    },

    "VM_TEST" : {
      "path" : "build/vmx86_test.jar",
      "subDir" : "vmx86",
      "sourcesPath" : "build/vmx86_test.src.zip",
      "dependencies" : [
        "org.graalvm.vm.x86.test"
      ],
      "distDependencies" : [
        "CORE",
        "POSIX",
        "VM",
        "VM_TESTCASES",
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_NFI"
      ],
      "exclude" : [
        "mx:JUNIT"
      ],
      "javaProperties" : {
        "vmx86test.testSuitePath" : "<path:VM_TESTCASES>"
      },
    }
  }
}
