/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.descriptors.findPackageFragmentForFile
import org.jetbrains.kotlin.utils.addIfNotNull

class ModuleGenerator(override val context: GeneratorContext) : Generator {

    private val constantValueGenerator = context.constantValueGenerator

    fun generateModuleFragment(ktFiles: Collection<KtFile>, deserializer: IrDeserializer, extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY): IrModuleFragment =
        generateModuleFragmentWithoutDependencies(ktFiles).also { irModule ->
            generateUnboundSymbolsAsDependencies(irModule, deserializer, extensions)
        }

    fun generateModuleFragmentWithoutDependencies(ktFiles: Collection<KtFile>): IrModuleFragment =
        IrModuleFragmentImpl(context.moduleDescriptor, context.irBuiltIns).also { irModule ->
            irModule.files.addAll(generateFiles(ktFiles))
        }

    fun generateUnboundSymbolsAsDependencies(
        irModule: IrModuleFragment,
        deserializer: IrDeserializer? = null,
        extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY
    ) {
        val fullIrProvidersList = generateTypicalIrProviderList(
            irModule.descriptor, context.irBuiltIns, context.symbolTable, deserializer,
            extensions
        )
        ExternalDependenciesGenerator(context.symbolTable, fullIrProvidersList, context.languageVersionSettings)
            .generateUnboundSymbolsAsDependencies()
    }

    fun generateUnboundSymbolsAsDependencies(irProviders: List<IrProvider>) {
        ExternalDependenciesGenerator(context.symbolTable, irProviders, context.languageVersionSettings)
            .generateUnboundSymbolsAsDependencies()
    }

    private fun generateFiles(ktFiles: Collection<KtFile>): List<IrFile> {
        val irDeclarationGenerator = DeclarationGenerator(context)

        return ktFiles.map { ktFile ->
            generateSingleFile(irDeclarationGenerator, ktFile)
        }
    }

    private fun generateSingleFile(irDeclarationGenerator: DeclarationGenerator, ktFile: KtFile): IrFileImpl {
        val irFile = createEmptyIrFile(ktFile)

        for (ktAnnotationEntry in ktFile.annotationEntries) {
            val annotationDescriptor = getOrFail(BindingContext.ANNOTATION, ktAnnotationEntry)
            constantValueGenerator.generateAnnotationConstructorCall(annotationDescriptor)?.let {
                irFile.annotations += it
            }
        }

        for (ktDeclaration in ktFile.declarations) {
            irFile.declarations.addIfNotNull(irDeclarationGenerator.generateMemberDeclaration(ktDeclaration))
        }

        return irFile
    }

    private fun createEmptyIrFile(ktFile: KtFile): IrFileImpl {
        val fileEntry = context.sourceManager.getOrCreateFileEntry(ktFile)
        val packageFragmentDescriptor = context.moduleDescriptor.findPackageFragmentForFile(ktFile)!!
        val irFile = IrFileImpl(fileEntry, packageFragmentDescriptor).apply {
            metadata = MetadataSource.File(CodegenUtil.getMemberDescriptorsToGenerate(ktFile, context.bindingContext))
        }
        context.sourceManager.putFileEntry(irFile, fileEntry)
        return irFile
    }
}
