package org.ton.intellij.func.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.func.psi.FuncGlobalVar
import org.ton.intellij.func.psi.impl.FuncGlobalVarImpl
import org.ton.intellij.func.stub.FuncGlobalVarStub
import org.ton.intellij.func.stub.index.FuncNamedElementIndex

class FuncGlobalVarStubElementType(
    debugName: String,
) : FuncNamedStubElementType<FuncGlobalVarStub, FuncGlobalVar>(debugName) {
    override fun serialize(stub: FuncGlobalVarStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): FuncGlobalVarStub {
        val name = dataStream.readName()
        return FuncGlobalVarStub(parentStub, this, name)
    }

    override fun createStub(
        psi: FuncGlobalVar,
        parentStub: StubElement<out PsiElement>,
    ): FuncGlobalVarStub {
        return FuncGlobalVarStub(parentStub, this, psi.name)
    }

    override fun createPsi(stub: FuncGlobalVarStub): FuncGlobalVar {
        return FuncGlobalVarImpl(stub, this)
    }

    override fun indexStub(stub: FuncGlobalVarStub, sink: IndexSink) {
        val name = stub.name ?: return
        sink.occurrence(FuncNamedElementIndex.KEY, name)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<FuncGlobalVar>()
        val ARRAY_FACTORY: ArrayFactory<FuncGlobalVar?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
