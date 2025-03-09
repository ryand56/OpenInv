package com.github.jikoo.openinv.specialsource

import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.RemapperProcessor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode

class ReflectionPreprocessor(
  private val jarMapping: JarMapping
) : RemapperProcessor(null, jarMapping, null) {

  companion object {
    private val JVM_TYPE_PATTERN = "^\\[*L.+;\$".toRegex()
  }

  override fun process(classReader: ClassReader): ByteArray {
    val classNode = ClassNode()
    classReader.accept(classNode, 0)

    for (methodNode in classNode.methods) {
      var insn = methodNode.instructions.first
      while (insn != null) {
        when (insn.opcode) {
          Opcodes.INVOKEVIRTUAL -> this.remapGetDeclaredField(insn)
          Opcodes.INVOKESTATIC -> this.remapClassForName(insn)
        }
        insn = insn.next
      }
    }

    val classWriter = ClassWriter(0)
    classNode.accept(classWriter)
    return classWriter.toByteArray()
  }

  private fun remapGetDeclaredField(insn: AbstractInsnNode) {
    val mi = insn as MethodInsnNode

    if (mi.owner != "java/lang/Class" || mi.name != "getDeclaredField" || mi.desc != "(Ljava/lang/String;)Ljava/lang/reflect/Field;") {
      return
    }

    this.logR("Found getDeclaredField!")
    if (insn.previous == null || insn.previous.opcode != Opcodes.LDC) {
      this.logR("- not constant field; skipping, prev=" + insn.getPrevious())
      return
    }
    val ldcField = insn.getPrevious() as LdcInsnNode
    if (ldcField.cst !is String) {
      this.logR("- not field string; skipping: ${ldcField.cst}")
      return
    }
    val fieldName = ldcField.cst as String
    if (ldcField.previous == null || ldcField.previous.opcode != Opcodes.LDC) {
      this.logR("- not constant class; skipping: field=$fieldName")
      return
    }
    val ldcClass = ldcField.previous as LdcInsnNode
    if (ldcClass.cst !is Type) {
      this.logR("- not class type; skipping: field=${ldcClass.cst}, class=${ldcClass.cst}")
      return
    }

    val className = (ldcClass.cst as Type).internalName
    val newName = lookup(className, fieldName)
    this.logR("Remapping $className/$fieldName -> $newName")
    if (newName != null) {
      ldcField.cst = newName
    }
  }

  private fun lookup(className: String, fieldName: String): String? {
    val key = "$className/$fieldName"

    // Try direct lookup first.
    val direct = jarMapping.fields[key]
    if (direct != null) {
      return direct
    }

    // Fall through to indirect lookup in case the mappings are from Proguard.
    for (entry in jarMapping.fields) {
      // This is a safe index check. We know the string starts with but does not equal the key.
      if (!entry.key.startsWith(key) || entry.key[key.length] != '/') {
        // Not the same class or field, name shares a common prefix.
        continue
      }

      val type = entry.key.substring(key.length + 1)
      // Type should be [*L<identifier>;
      if (type.length >= 3 && type.matches(JVM_TYPE_PATTERN)) {
        return entry.value
      }
    }

    // No match.
    return null
  }

  private fun remapClassForName(insn: AbstractInsnNode) {
    val mi = insn as MethodInsnNode

    if (mi.owner != "java/lang/Class" || mi.name != "forName" || mi.desc != "(Ljava/lang/String;)Ljava/lang/Class;") {
      return
    }
    this.logR("Found Class forName!")
    if (insn.getPrevious() == null || insn.getPrevious().opcode != Opcodes.LDC) {
      this.logR("- not constant field; skipping, prev=${insn.previous}")
      return
    }
    val ldcClassName = insn.getPrevious() as LdcInsnNode
    if (ldcClassName.cst !is String) {
      this.logR("- not field string; skipping: " + ldcClassName.cst)
      return
    }

    val className = ldcClassName.cst as String
    val newName = jarMapping.classes[className.replace('.', '/')]
    this.logR("Remapping $className -> $newName")
    if (newName != null) {
      ldcClassName.cst = newName.replace('/', '.')
    }
  }

  private fun logR(message: String) {
    if (this.debug) {
      println("[ReflectionRemapper] $message")
    }
  }

}
