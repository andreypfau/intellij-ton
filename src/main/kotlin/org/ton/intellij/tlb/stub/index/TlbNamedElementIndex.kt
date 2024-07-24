package org.ton.intellij.tlb.stub.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.tlb.psi.TlbNamedElement
import org.ton.intellij.tlb.stub.TlbFileStub
import org.ton.intellij.tlb.stub.TlbNamedStub
import org.ton.intellij.util.checkCommitIsNotInProgress
import org.ton.intellij.util.getElements

class TlbNamedElementIndex<T : TlbNamedStub<*>> : StringStubIndexExtension<TlbNamedElement>() {
    override fun getVersion(): Int = TlbFileStub.Type.stubVersion

    override fun getKey(): StubIndexKey<String, TlbNamedElement> = KEY

    companion object {
        val KEY =
            StubIndexKey.createIndexKey<String, TlbNamedElement>("org.ton.intellij.tlb.stub.index.TlbNamedElementIndex")

        fun findElementsByName(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<TlbNamedElement> {
            checkCommitIsNotInProgress(project)
            return getElements(KEY, target, project, scope)
        }
    }
}
