package io.kurumi.ntt.fragment.sticker;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.pengrad.telegrambot.request.AddStickerToSet;
import com.pengrad.telegrambot.request.GetStickerSet;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.GetStickerSetResponse;
import io.kurumi.ntt.Env;
import io.kurumi.ntt.db.PointData;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.utils.BotLog;
import io.kurumi.ntt.utils.Html;
import java.io.File;
import java.io.IOException;
import net.coobird.thumbnailator.Thumbnails;

public class AddSticker extends Fragment {

	@Override
	public void init(BotFragment origin) {

		super.init(origin);

		registerFunction("add_sticker");
		registerPoint(POINT_ADD_STICKER);

	}

	class StickerAdd extends PointData {

		int type = 0;

		String setName;

		File sticker;

	}

	final String POINT_ADD_STICKER = "add_sticker";

	@Override
	public void onFunction(UserData user,Msg msg,String function,String[] params) {

		PointData data = new StickerAdd().with(msg);
		
		setPrivatePoint(user,POINT_ADD_STICKER,data);

		msg.send("要添加到哪个贴纸包呢？请发送简称/链接 或 贴纸包里的一张贴纸 ~").withCancel().exec(data);

	}

	@Override
	public void onPoint(UserData user,Msg msg,String point,PointData data) {

		StickerAdd add = (StickerAdd)data.with(msg);

		if (add.type == 0) {

			String target;

			if (msg.hasText()) {
				
				target = msg.text();
				
				if (target.contains("/")) target = StrUtil.subAfter(target,"/",true);

			} else if (msg.message().sticker() != null) {

				target = msg.message().sticker().setName();

				if (target == null) {

					msg.send("这个贴纸没有贴纸包... 请重试 :)").withCancel().exec(data);

					return;

				}

			} else {

				msg.send("请发送 目标贴纸包的简称或链接 或目标贴纸包的任意贴纸 : ").withCancel().exec(data);

				return;

			}

			if (!target.toLowerCase().endsWith("_by_" + origin.me.username().toLowerCase())) {

				msg.send("对不起，但是根据 " + NewStickerSet.DOC + " , NTT只能操作由NTT创建的贴纸包。 请重新选择 :").withCancel().html().exec(data);

				return;
				
			}

			add.type = 1;
			add.setName = target;

			msg.send("好，现在发送任意贴纸 / 图片 / 图片文件 :","不建议直接发送图片，会被压到看不清").withCancel().exec(data);

		} else if (msg.message().sticker() != null) {

			BaseResponse resp = bot().execute(new AddStickerToSet(user.id.intValue(),add.setName,readStiker(user.id,msg.message().sticker()),msg.message().sticker().emoji()));

			if (!resp.isOk()) {

				msg.send("添加失败！请重试",resp.description()).withCancel().exec(data);

				return;

			}

			msg.reply("添加成功！重启客户端生效"," 退出添加使用 /cancel").exec(data);
			
		} else if (msg.message().photo() != null || msg.message().document() != null) {

			File photo = msg.message().photo() != null ? msg.photo() : msg.file();

			if (photo == null) {

				msg.send("文件下载失败... 请重试").withCancel().exec(data);

				return;

			}

			File local = new File(Env.CACHE_DIR,"sticker_convert_cache/" + (msg.message().photo() != null ? msg.message().photo()[0].fileId() : msg.doc().fileId()) + ".png");

			if (!local.isFile()) {

				local.getParentFile().mkdirs();

				try {

					Thumbnails
						.of(photo)
						.size(512,512)
						.outputQuality(1f)
						.outputFormat("png")
						.toFile(local);

					if (local.length() > 512 * 1024) {

						float outSize = ((512 * 1024) / local.length())/* - 0.3f*/;

						Thumbnails.of(local).outputQuality(outSize).toFile(local);

					}


				} catch (IOException e) {

					msg.send("转码失败 : " + BotLog.parseError(e)).exec(data);

					return;

				}


			}

			add.sticker = local;
			add.type = 2;

			msg.send("输入代表该贴纸的Emoji表情 :").withCancel().exec(data);

		} else if (add.type == 2) {
			
			if (!msg.hasText()) {

				msg.send("请输入代表该贴纸的Emoji表情 :").withCancel().exec(data);

				return;

			}

			add.type = 1;

			Msg status = msg.send("正在添加贴纸...").send();

			BaseResponse resp = bot().execute(new AddStickerToSet(user.id.intValue(),add.setName,FileUtil.readBytes(add.sticker),msg.text()));

			if (!resp.isOk()) {

				status.edit("添加失败 请重试 : " + resp.description()).exec();
				
				return;

			}
			
			msg.reply("添加成功！重启客户端生效"," 退出添加使用 /cancel").exec(data);
	
		} else {
			
			msg.send("正在添加贴纸 请发送贴纸 / 图片 / 图片文件 : ").withCancel().exec(data);
			
		}

	}

}