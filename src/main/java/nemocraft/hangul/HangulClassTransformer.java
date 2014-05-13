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
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

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
				transformedName.equals("net.minecraft.client.gui.inventory.GuiContainerCreative") ||
				// Applied Energistics
				name.equals("appeng.me.gui.GuiPreformatter") ||
				name.equals("appeng.me.gui.GuiTerminal") ||
				// IC2 Nuclear Control
				name.equals("shedar.mods.ic2.nuclearcontrol.gui.GuiAdvancedInfoPanel") ||
				name.equals("shedar.mods.ic2.nuclearcontrol.gui.GuiIC2Thermo") ||
				name.equals("shedar.mods.ic2.nuclearcontrol.gui.GuiInfoPanel") ||
				name.equals("shedar.mods.ic2.nuclearcontrol.gui.GuiRemoteThermo"))
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

		// IC2 Nuclear Control 텍스트 카드의 GuiTextArea를 대체 클래스로 변경
		if (name.equals("shedar.mods.ic2.nuclearcontrol.gui.GuiCardText"))
		{
			return patchIC2NCGuiTextArea(bytes);
		}

		// AE 터미널의 아이템 목록에서 큰 폰트를 사용할 때 폰트 크기 증가
		if (name.equals("appeng.common.AppEngRenderItem"))
		{
			return patchAERenderItem(bytes);
		}

		// AE 컨트롤러 GUI의 폰트 크기 증가
		if (name.equals("appeng.me.gui.GuiController"))
		{
			return patchAEGuiController(bytes);
		}

		// VoxelMap Add Waypoint의 GuiTextField를 대체 클래스로 변경
		if (name.equals("com.thevoxelbox.voxelmap.gui.GuiScreenAddWaypoint") ||
				name.equals("com.thevoxelbox.voxelmap.util.GuiScreenAddWaypoint"))
		{
			return patchVMapGuiTextField(bytes);
		}

		// 표지판 내용을 확인할 때 allowedCharactes.indexOf를 사용할 경우 유니코드/한글 부분이 삭제되기에 isAllowedCharacter 메소드 호출로 변경
		// Server Side
		if (transformedName.equals("net.minecraft.network.NetServerHandler"))
		{
			return patchNetServerHandler(bytes);
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

					if (!FMLDeobfuscatingRemapper.INSTANCE.map(node.desc).equals("net/minecraft/client/gui/GuiTextField"))
						continue;

					m.instructions.set(node, new TypeInsnNode(node.getOpcode(), "nemocraft/hangul/MCGuiTextField"));
				}
				// 객체 생성자 호출 부분 치환
				else if (insn instanceof MethodInsnNode)
				{
					MethodInsnNode node = (MethodInsnNode)insn;

					if (!node.name.equals("<init>") ||
							!FMLDeobfuscatingRemapper.INSTANCE.map(node.owner).equals("net/minecraft/client/gui/GuiTextField"))
						continue;

					m.instructions.set(node,
							new MethodInsnNode(node.getOpcode(), "nemocraft/hangul/MCGuiTextField", node.name, node.desc));
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
		targets.add("displayGUIEditSign");
		targets.add("func_71014_a");
		targets.add("displayGUIBook");
		targets.add("func_71048_c");

		HashMap<String, String> replaces = new HashMap<String, String>();
		replaces.put("net/minecraft/client/gui/inventory/GuiEditSign", "nemocraft/hangul/MCGuiEditSign");
		replaces.put("net/minecraft/client/gui/GuiScreenBook", "nemocraft/hangul/MCGuiScreenBook");

		for (MethodNode m: classNode.methods)
		{
			String deobf = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(classNode.name, m.name, m.desc);

			if (!targets.contains(deobf))
				continue;

			Iterator<AbstractInsnNode> instructions = m.instructions.iterator();
			while (instructions.hasNext())
			{
				AbstractInsnNode insn = instructions.next();

				// 객체 생성 부분 치환
				if (insn.getOpcode() == Opcodes.NEW)
				{
					TypeInsnNode node = (TypeInsnNode)insn;

					deobf = FMLDeobfuscatingRemapper.INSTANCE.map(node.desc);
					if (!replaces.containsKey(deobf))
						continue;

					m.instructions.set(node, new TypeInsnNode(node.getOpcode(), replaces.get(deobf)));
				}
				// 객체 메소드 호출 부분 치환
				else if (insn instanceof MethodInsnNode)
				{
					MethodInsnNode node = (MethodInsnNode)insn;

					deobf = FMLDeobfuscatingRemapper.INSTANCE.map(node.owner);
					if (!replaces.containsKey(deobf))
						continue;

					m.instructions.set(node, new MethodInsnNode(node.getOpcode(), replaces.get(deobf), node.name, node.desc));
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
			String deobf = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(classNode.name, m.name, m.desc);

			if (!deobf.equals("getCharWidth") && !deobf.equals("func_78263_a") &&
					!deobf.equals("renderUnicodeChar") && !deobf.equals("func_78277_a"))
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

					deobf = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(field.owner, field.name, field.desc);
					if (!deobf.equals("glyphWidth") && !deobf.equals("field_78287_e"))
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

					if (!node.name.equals("<init>") ||
							!node.owner.equals("codechicken/nei/SearchField"))
						continue;

					m.instructions.set(node,
							new MethodInsnNode(node.getOpcode(), "nemocraft/hangul/NEISearchField", node.name, node.desc));
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private byte[] patchIC2NCGuiTextArea(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		for (MethodNode m: classNode.methods)
		{
			if (!m.name.equals("initControls"))
				continue;

			Iterator<AbstractInsnNode> instructions = m.instructions.iterator();
			while (instructions.hasNext())
			{
				AbstractInsnNode insn = instructions.next();

				// 객체 생성 부분 치환
				if (insn.getOpcode() == Opcodes.NEW)
				{
					TypeInsnNode node = (TypeInsnNode)insn;

					if (!node.desc.equals("shedar/mods/ic2/nuclearcontrol/gui/GuiTextArea"))
						continue;

					m.instructions.set(node, new TypeInsnNode(node.getOpcode(), "nemocraft/hangul/IC2NCGuiTextArea"));
				}
				// 객체 생성자 호출 부분 치환
				else if (insn instanceof MethodInsnNode)
				{
					MethodInsnNode node = (MethodInsnNode)insn;

					if (!node.name.equals("<init>") ||
							!node.owner.equals("shedar/mods/ic2/nuclearcontrol/gui/GuiTextArea"))
						continue;

					m.instructions.set(node,
							new MethodInsnNode(node.getOpcode(), "nemocraft/hangul/IC2NCGuiTextArea", node.name, node.desc));
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private byte[] patchAERenderItem(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		for (MethodNode m: classNode.methods)
		{
			if (!m.name.equals("renderItemOverlayIntoGUI") && !m.name.equals("func_94148_a"))
				continue;

			for (int index = 0; index < m.instructions.size(); index++)
			{
				AbstractInsnNode insn = m.instructions.get(index);

				// 0.85F를 1.0F로 변경
				if (insn.getOpcode() == Opcodes.LDC)
				{
					LdcInsnNode node = (LdcInsnNode)insn;

					if (!(node.cst instanceof Float) || ((Float)node.cst != 0.85F))
						continue;

					m.instructions.set(node, new LdcInsnNode(new Float(1.0F)));
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private byte[] patchAEGuiController(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		for (MethodNode m: classNode.methods)
		{
			if (!m.name.equals("drawGuiForegroundLayer"))
				continue;

			for (int index = 0; index < m.instructions.size(); index++)
			{
				AbstractInsnNode insn = m.instructions.get(index);

				// glScaled의 0.5D, 0.9D를 1.0D로 변경
				if (insn.getOpcode() == Opcodes.LDC &&
						m.instructions.get(index + 1).getOpcode() == Opcodes.LDC &&
						m.instructions.get(index + 2).getOpcode() == Opcodes.LDC)
				{
					LdcInsnNode node1 = (LdcInsnNode)insn;
					LdcInsnNode node2 = (LdcInsnNode)m.instructions.get(index + 1);
					LdcInsnNode node3 = (LdcInsnNode)m.instructions.get(index + 2);

					if ((!(node1.cst instanceof Double) || (((Double)node1.cst != 0.9D) && ((Double)node1.cst != 0.5D))) ||
							(!(node2.cst instanceof Double) || (((Double)node2.cst != 0.9D) && ((Double)node2.cst != 0.5D))) ||
							(!(node3.cst instanceof Double) || (((Double)node3.cst != 0.9D) && ((Double)node3.cst != 0.5D))))
						continue;

					m.instructions.set(node1, new LdcInsnNode(new Double(1.0D)));
					m.instructions.set(node2, new LdcInsnNode(new Double(1.0D)));
					m.instructions.set(node3, new LdcInsnNode(new Double(1.0D)));
				}

				if (insn.getOpcode() == Opcodes.LDC &&
						m.instructions.get(index + 1).getOpcode() == Opcodes.DMUL)
				{
					LdcInsnNode node = (LdcInsnNode)insn;

					if (!(node.cst instanceof Double) || ((Double)node.cst != 2.0D))
						continue;

					m.instructions.set(node, new LdcInsnNode(new Double(1.0D)));
				}

				if (insn.getOpcode() == Opcodes.BIPUSH &&
						m.instructions.get(index + 1).getOpcode() == Opcodes.IADD &&
						m.instructions.get(index + 2).getOpcode() == Opcodes.ICONST_2 &&
						m.instructions.get(index + 3).getOpcode() == Opcodes.IMUL)
				{
					IntInsnNode node = (IntInsnNode)insn;

					if (node.operand != 6)
						continue;

					m.instructions.set(node, new IntInsnNode(Opcodes.BIPUSH, 4));
					m.instructions.set(m.instructions.get(index + 2), new InsnNode(Opcodes.ICONST_1));
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
			String deobf = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(classNode.name, m.name, m.desc);

			if (!deobf.equals("initGui") && !deobf.equals("func_73866_w_"))
				continue;

			for (int index = 0; index < m.instructions.size(); index++)
			{
				AbstractInsnNode insn = m.instructions.get(index);

				// 객체 생성 부분 치환
				if (insn.getOpcode() == Opcodes.NEW)
				{
					TypeInsnNode node = (TypeInsnNode)insn;

					if (!FMLDeobfuscatingRemapper.INSTANCE.map(node.desc).equals("net/minecraft/client/gui/GuiTextField"))
						continue;

					newGuiTextField = node;
				}
				// 객체 생성자 호출 부분 치환
				else if (insn instanceof MethodInsnNode)
				{
					if (newGuiTextField == null)
						continue;

					MethodInsnNode node = (MethodInsnNode)insn;

					if (!node.name.equals("<init>") ||
							!FMLDeobfuscatingRemapper.INSTANCE.map(node.owner).equals("net/minecraft/client/gui/GuiTextField"))
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
					m.instructions.set(node,
							new MethodInsnNode(node.getOpcode(), "nemocraft/hangul/MCGuiTextField", node.name, node.desc));
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private byte[] patchNetServerHandler(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		MethodNode handleChat = null;
		MethodNode handleUpdateSign = null;
		MethodInsnNode isAllowedCharacter = null;

		for (MethodNode m: classNode.methods)
		{
			String deobf = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(classNode.name, m.name, m.desc);;

			if (deobf.equals("handleChat") || deobf.equals("func_72481_a"))
				handleChat = m;
			if (deobf.equals("handleUpdateSign") || deobf.equals("func_72487_a"))
				handleUpdateSign = m;

			if ((handleChat != null) && (handleUpdateSign != null))
				break;
		}

		if ((handleChat == null) || (handleUpdateSign == null))
		{
			System.out.println("[Error] handleChat or handleUpdateSign method not found");
			return bytes;
		}

		// handleChat 메소드에서 isAllowedCharacter 의 난독화된 정보를 확인
		for (int index = 0; index < handleChat.instructions.size(); index++)
		{
			if (handleChat.instructions.get(index).getOpcode() == Opcodes.INVOKESTATIC)
			{
				MethodInsnNode invoke = (MethodInsnNode)handleChat.instructions.get(index);
				if (!invoke.desc.equals("(C)Z"))
					continue;

				String deobf = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(invoke.owner, invoke.name, invoke.desc);
				if (!deobf.equals("isAllowedCharacter") && !deobf.equals("func_71566_a"))
					continue;

				isAllowedCharacter = invoke;
				break;
			}
		}

		if (isAllowedCharacter == null)
		{
			System.out.println("[Error] isAllowedCharacter instruction not found");
			return bytes;
		}

		// handleUpdateSign 메소드에서 allowedCharacters.indexOf 호출 부분을 isAllowedCharacter 호출로 변경
		for (int index = 0; index < handleUpdateSign.instructions.size(); index++)
		{
			if (handleUpdateSign.instructions.get(index).getOpcode() == Opcodes.GETSTATIC &&
					handleUpdateSign.instructions.get(index + 7).getOpcode() == Opcodes.INVOKEVIRTUAL &&
					handleUpdateSign.instructions.get(index + 8).getOpcode() == Opcodes.IFGE)
			{
				FieldInsnNode old_field = (FieldInsnNode)handleUpdateSign.instructions.get(index);
				MethodInsnNode old_invoke = (MethodInsnNode)handleUpdateSign.instructions.get(index + 7);
				JumpInsnNode old_jump = (JumpInsnNode)handleUpdateSign.instructions.get(index + 8);

				String deobf = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(old_field.owner, old_field.name, old_field.desc);
				if (!deobf.equals("allowedCharacters") && !deobf.equals("field_71568_a"))
					continue;

				handleUpdateSign.instructions.set(old_invoke,
						new MethodInsnNode(Opcodes.INVOKESTATIC, isAllowedCharacter.owner, isAllowedCharacter.name, isAllowedCharacter.desc));
				handleUpdateSign.instructions.set(old_jump, new JumpInsnNode(Opcodes.IFNE, old_jump.label));
				handleUpdateSign.instructions.remove(old_field);
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}
}
