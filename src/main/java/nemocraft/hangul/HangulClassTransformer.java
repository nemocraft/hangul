package nemocraft.hangul;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * 마인크래프트 기본 클래스 교체용 ASM transformer
 *
 * @author nemocraft
 */
public class HangulClassTransformer implements IClassTransformer
{
	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes)
	{
		// GuiTextField를 대체 클래스로 변경
		if (transformedName.equals("net.minecraft.client.gui.GuiChat") ||
				transformedName.equals("net.minecraft.client.gui.GuiCommandBlock") ||
				transformedName.equals("net.minecraft.client.gui.GuiRepair") ||
				transformedName.equals("net.minecraft.client.gui.inventory.GuiContainerCreative"))
		{
			return patchGuiTextField(bytes);
		}

		// GuiEditSign, GuiScreenBook를 대체 클래스로 변경
		if (transformedName.equals("net.minecraft.client.entity.EntityPlayerSP"))
		{
			return patchDisplayGui(bytes);
		}

		// FontRenderer의 글자폭 계산 오류를 수정
		if (transformedName.equals("net.minecraft.client.gui.FontRenderer"))
		{
			return patchFontRenderer(bytes);
		}

		// NEI 검색창의 SearchField를 대체 클래스로 변경
		if (name.equals("codechicken.nei.LayoutManager"))
		{
			return patchNEISearchField(bytes);
		}

		// AE2 터미널의 MEGuiTextField를 대체 클래스로 변경
		if (name.equals("appeng.client.gui.implementations.GuiMEMonitorable"))
		{
			return patchAEMEGuiTextField(bytes);
		}

		// VoxelMap Add Waypoint의 GuiTextField를 대체 클래스로 변경
		if (name.equals("com.thevoxelbox.voxelmap.gui.GuiScreenAddWaypoint") ||
				name.equals("com.thevoxelbox.voxelmap.util.GuiScreenAddWaypoint"))
		{
			return patchVMapGuiTextField(bytes);
		}

		return bytes;
	}

	private byte[] patchGuiTextField(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		// 클래스 내부의 메소드 확인
		for (MethodNode m: classNode.methods)
		{
			Iterator<AbstractInsnNode> instructions = m.instructions.iterator();
			while (instructions.hasNext())
			{
				AbstractInsnNode insn = instructions.next();

				// 객체 생성 부분 치환
				if (insn.getOpcode() == Opcodes.NEW)
				{
					TypeInsnNode node = (TypeInsnNode)insn;

					if (!node.desc.equals("net/minecraft/client/gui/GuiTextField"))
						continue;

					m.instructions.set(node, new TypeInsnNode(node.getOpcode(), "nemocraft/hangul/MCGuiTextField"));
				}
				// 객체 생성자 호출 부분 치환
				else if (insn instanceof MethodInsnNode)
				{
					MethodInsnNode node = (MethodInsnNode)insn;

					if (!node.name.equals("<init>") || !node.owner.equals("net/minecraft/client/gui/GuiTextField"))
						continue;

					m.instructions.set(node, new MethodInsnNode(node.getOpcode(), "nemocraft/hangul/MCGuiTextField", node.name, node.desc));
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private byte[] patchDisplayGui(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		ArrayList<String> targets = new ArrayList<String>();
		targets.add("func_146100_a");
		targets.add("displayGUIBook");
		targets.add("func_71048_c");

		HashMap<String, String> replaces = new HashMap<String, String>();
		replaces.put("net/minecraft/client/gui/inventory/GuiEditSign", "nemocraft/hangul/MCGuiEditSign");
		replaces.put("net/minecraft/client/gui/GuiScreenBook", "nemocraft/hangul/MCGuiScreenBook");

		for (MethodNode m: classNode.methods)
		{
			if (!targets.contains(m.name))
				continue;

			Iterator<AbstractInsnNode> instructions = m.instructions.iterator();
			while (instructions.hasNext())
			{
				AbstractInsnNode insn = instructions.next();

				// 객체 생성 부분 치환
				if (insn.getOpcode() == Opcodes.NEW)
				{
					TypeInsnNode node = (TypeInsnNode)insn;

					if (!replaces.containsKey(node.desc))
						continue;

					m.instructions.set(node, new TypeInsnNode(node.getOpcode(), replaces.get(node.desc)));
				}
				// 객체 메소드 호출 부분 치환
				else if (insn instanceof MethodInsnNode)
				{
					MethodInsnNode node = (MethodInsnNode)insn;

					if (!replaces.containsKey(node.owner))
						continue;

					m.instructions.set(node, new MethodInsnNode(node.getOpcode(), replaces.get(node.owner), node.name, node.desc));
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private byte[] patchFontRenderer(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		for (MethodNode m: classNode.methods)
		{
			if (!m.name.equals("getCharWidth") && !m.name.equals("func_78263_a") &&
					!m.name.equals("renderUnicodeChar") && !m.name.equals("func_78277_a"))
				continue;

			for (int index = 0; index < m.instructions.size(); index++)
			{
				AbstractInsnNode insn = m.instructions.get(index);
				// glyphWidth[char] >>> 4 부분에 & 15 연산 추가
				if (insn.getOpcode() == Opcodes.GETFIELD &&
						m.instructions.get(index + 2).getOpcode() == Opcodes.BALOAD &&
						m.instructions.get(index + 4).getOpcode() == Opcodes.IUSHR &&
						m.instructions.get(index + 5).getOpcode() == Opcodes.ISTORE)
				{
					FieldInsnNode field = (FieldInsnNode)insn;
					AbstractInsnNode node = m.instructions.get(index + 5);

					if (!field.name.equals("glyphWidth") && !field.name.equals("field_78287_e"))
						continue;

					m.instructions.insertBefore(node, new IntInsnNode(Opcodes.BIPUSH, 15));
					m.instructions.insertBefore(node, new InsnNode(Opcodes.IAND));
				}

				// 글자폭 7 초과일 경우 무조건 15로 바꿔버리는 부분을 15 초과일 경우로 변경하여 글자폭 변경 부분을 동작하지 않도록 함
				if (insn.getOpcode() == Opcodes.BIPUSH)
				{
					IntInsnNode node = (IntInsnNode)insn;

					if (node.operand != 7)
						continue;

					m.instructions.set(node, new IntInsnNode(node.getOpcode(), 15));
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private byte[] patchNEISearchField(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		for (MethodNode m: classNode.methods)
		{
			if (!m.name.equals("init"))
				continue;

			Iterator<AbstractInsnNode> instructions = m.instructions.iterator();
			while (instructions.hasNext())
			{
				AbstractInsnNode insn = instructions.next();

				// 객체 생성 부분 치환
				if (insn.getOpcode() == Opcodes.NEW)
				{
					TypeInsnNode node = (TypeInsnNode)insn;

					if (!node.desc.equals("codechicken/nei/SearchField"))
						continue;

					m.instructions.set(node, new TypeInsnNode(node.getOpcode(), "nemocraft/hangul/NEISearchField"));
				}
				// 객체 생성자 호출 부분 치환
				else if (insn instanceof MethodInsnNode)
				{
					MethodInsnNode node = (MethodInsnNode)insn;

					if (!node.name.equals("<init>") || !node.owner.equals("codechicken/nei/SearchField"))
						continue;

					m.instructions.set(node, new MethodInsnNode(node.getOpcode(), "nemocraft/hangul/NEISearchField", node.name, node.desc));
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private byte[] patchAEMEGuiTextField(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		// 클래스 내부의 메소드 확인
		for (MethodNode m: classNode.methods)
		{
			Iterator<AbstractInsnNode> instructions = m.instructions.iterator();
			while (instructions.hasNext())
			{
				AbstractInsnNode insn = instructions.next();

				// 객체 생성 부분 치환
				if (insn.getOpcode() == Opcodes.NEW)
				{
					TypeInsnNode node = (TypeInsnNode)insn;

					if (!node.desc.equals("appeng/client/gui/widgets/MEGuiTextField"))
						continue;

					m.instructions.set(node, new TypeInsnNode(node.getOpcode(), "nemocraft/hangul/AEMEGuiTextField"));
				}
				// 객체 생성자 호출 부분 치환
				else if (insn instanceof MethodInsnNode)
				{
					MethodInsnNode node = (MethodInsnNode)insn;

					if (!node.name.equals("<init>") || !node.owner.equals("appeng/client/gui/widgets/MEGuiTextField"))
						continue;

					m.instructions.set(node, new MethodInsnNode(node.getOpcode(), "nemocraft/hangul/AEMEGuiTextField", node.name, node.desc));
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private byte[] patchVMapGuiTextField(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		TypeInsnNode newGuiTextField = null;

		// 클래스 내부의 메소드 확인
		for (MethodNode m: classNode.methods)
		{
			if (!m.name.equals("initGui") && !m.name.equals("func_73866_w_"))
				continue;

			for (int index = 0; index < m.instructions.size(); index++)
			{
				AbstractInsnNode insn = m.instructions.get(index);

				// 객체 생성 부분 치환
				if (insn.getOpcode() == Opcodes.NEW)
				{
					TypeInsnNode node = (TypeInsnNode)insn;

					if (!node.desc.equals("net/minecraft/client/gui/GuiTextField"))
						continue;

					newGuiTextField = node;
				}
				// 객체 생성자 호출 부분 치환
				else if (insn instanceof MethodInsnNode)
				{
					if (newGuiTextField == null)
						continue;

					MethodInsnNode node = (MethodInsnNode)insn;

					if (!node.name.equals("<init>") || !node.owner.equals("net/minecraft/client/gui/GuiTextField"))
						continue;

					TypeInsnNode pair = newGuiTextField;
					newGuiTextField = null;

					AbstractInsnNode next = m.instructions.get(index + 1);
					if (next.getOpcode() != Opcodes.PUTFIELD)
						continue;

					FieldInsnNode put = (FieldInsnNode)next;

					if (!put.name.equals("waypointName"))
						continue;

					m.instructions.set(pair, new TypeInsnNode(pair.getOpcode(), "nemocraft/hangul/MCGuiTextField"));
					m.instructions.set(node, new MethodInsnNode(node.getOpcode(), "nemocraft/hangul/MCGuiTextField", node.name, node.desc));
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}
}
