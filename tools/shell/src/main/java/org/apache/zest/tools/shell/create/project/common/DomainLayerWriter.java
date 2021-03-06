/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package org.apache.zest.tools.shell.create.project.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import org.apache.zest.tools.shell.FileUtils;

public class DomainLayerWriter
{

    public void writeClass( Map<String, String> properties )
        throws IOException
    {
        String rootPackage = properties.get( "root.package" );
        String projectName = properties.get( "project.name" );
        try (PrintWriter pw = createPrinter( properties ))
        {
            pw.print( "package " );
            pw.print( properties.get( "root.package" ) );
            pw.println( ".bootstrap.domain;" );
            pw.println();
            pw.println(
                "import java.util.function.Function;\n" +
                "import org.apache.zest.api.structure.Application;\n" +
                "import org.apache.zest.api.structure.Module;\n" +
                "import org.apache.zest.bootstrap.AssemblyException;\n" +
                "import org.apache.zest.bootstrap.LayerAssembly;\n" +
                "import org.apache.zest.bootstrap.layered.LayerAssembler;\n" +
                "import org.apache.zest.bootstrap.layered.LayeredLayerAssembler;\n" +
                "\n" +
                "public class DomainLayer extends LayeredLayerAssembler\n" +
                "    implements LayerAssembler\n" +
                "{\n" +
                "    @Override\n" +
                "    public LayerAssembly assemble(LayerAssembly layer)\n" +
                "        throws AssemblyException\n" +
                "    {\n" +
                "        createModule( layer, CrudModule.class );\n" +
                "        createModule( layer, OrderModule.class );   // This is a simple sample that you typically remove.\n" +
                "        createModule( layer, SecurityModule.class );   // This is a simple sample that you typically remove.\n" +
                "        return layer;\n" +
                "    }\n" +
                "\n" +
                "    public static Function<Application, Module> typeFinder()\n" +
                "    {\n" +
                "        return application -> application.findModule( \"Domain Layer\", \"Assets Module\" );\n" +
                "    }\n" +
                "}\n"
            );
        }
    }

    private PrintWriter createPrinter( Map<String, String> properties )
        throws IOException
    {
        String packagename = properties.get( "root.package" ).replaceAll( "\\.", "/" ) + "/bootstrap/domain/";
        String classname = "DomainLayer";
        return FileUtils.createJavaClassPrintWriter( properties, "bootstrap", packagename, classname );
    }
}
