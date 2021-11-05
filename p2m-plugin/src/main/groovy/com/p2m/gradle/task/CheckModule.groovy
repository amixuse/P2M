package com.p2m.gradle.task

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets

class CheckModule extends DefaultTask {


    private RegularFileProperty propertiesFile = project.objects.fileProperty()

    @InputFile
    RegularFileProperty getPropertiesFile() {
        return propertiesFile
    }

    @TaskAction
    void doCheck(){
        def file = propertiesFile.get().asFile
        file.newReader(StandardCharsets.UTF_8.name()).eachLine { line ->
            def split = line.split("=")
            def attr = split[0]
            def value = split[1]
            if (attr == "genModuleInitSource") {
                def exist = value.toBoolean()
                if (!exist) {
                    throw project.logger.error("""
Must add source code in Module[${project.p2mProject.getModuleName()}]：

@ModuleInitializer
class ${project.p2mProject.getModuleName()}ModuleInit : ModuleInit{

    override fun onEvaluate(context: Context, taskRegister: TaskRegister<out TaskUnit>) { }

    override fun onExecuted(context: Context, taskOutputProvider: TaskOutputProvider) { }
}
""")
                }
            }
        }
    }

}
