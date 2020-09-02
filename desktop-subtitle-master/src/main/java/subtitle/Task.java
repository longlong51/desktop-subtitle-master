package subtitle;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alimt.model.v20181012.TranslateECommerceRequest;
import com.aliyuncs.alimt.model.v20181012.TranslateECommerceResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;

import javafx.application.Platform;
import javafx.scene.control.Label;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;

/**
 * Created by yi-ge 2018-12-21 23:46
 */
class Task implements Runnable {
	Label labelOriginal;
	Label labelTranslate;

	private String appKey = "";
	// private String accessToken = "";
	NlsClient client;

	public Task(Label labelOriginal,Label labelTranslate) {
		this.labelOriginal = labelOriginal;
		this.labelTranslate = labelTranslate;
	}

	public SpeechTranscriberListener getTranscriberListener() {
		SpeechTranscriberListener listener = new SpeechTranscriberListener() {

			// 识别出中间结果.服务端识别出一个字或词时会返回此消息.仅当setEnableIntermediateResult(true)时,才会有此类消息返回
			@Override
			public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
				System.out.println("name: " + response.getName() +
						// 状态码 20000000 表示正常识别
						", status: " + response.getStatus() +
						// 句子编号，从1开始递增
						", index: " + response.getTransSentenceIndex() +
						// 当前句子的中间识别结果
						", result: " + response.getTransSentenceText() +
						// 当前已处理的音频时长，单位是毫秒
						", time: " + response.getTransSentenceTime());

				final String r = response.getTransSentenceText();
				final String t=this.translate(r);
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						// Update UI here.
						labelOriginal.setText(r);
						labelTranslate.setText(t);
					}
				});
			}

			// 识别出一句话.服务端会智能断句,当识别到一句话结束时会返回此消息
			@Override
			public void onSentenceEnd(SpeechTranscriberResponse response) {
				System.out.println("name: " + response.getName() +
						// 状态码 20000000 表示正常识别
						", status: " + response.getStatus() +
						// 句子编号，从1开始递增
						", index: " + response.getTransSentenceIndex() +
						// 当前句子的完整识别结果
						", result: " + response.getTransSentenceText() +
						// 当前已处理的音频时长，单位是毫秒
						", time: " + response.getTransSentenceTime() +
						// SentenceBegin事件的时间，单位是毫秒
						", begin time: " + response.getSentenceBeginTime() +
						// 识别结果置信度，取值范围[0.0, 1.0]，值越大表示置信度越高
						", confidence: " + response.getConfidence());

				final String r = response.getTransSentenceText();
				final String t=this.translate(r);
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						// Update UI here.
						labelOriginal.setText(r);
						labelTranslate.setText(t);
					}
				});
			}

			// 识别出一句话就进行翻译
			public String translate(String sentence) {
				String accessKeyId = "";// 使用您的阿里云访问密钥
																// AccessKeyId
				String accessKeySecret = ""; // 使用您的阿里云访问密钥
				String translatedSentence = "";
				// 创建DefaultAcsClient实例并初始化
				try {
					DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", // 地域ID
							accessKeyId, // 阿里云账号的AccessKey ID
							accessKeySecret); // 阿里云账号Access Key Secret
					IAcsClient client = new DefaultAcsClient(profile);
					// 创建API请求并设置参数
					TranslateECommerceRequest eCommerceRequest = new TranslateECommerceRequest();
					eCommerceRequest.setScene("title");
					eCommerceRequest.setMethod(MethodType.POST); // 设置请求方式，POST
					eCommerceRequest.setFormatType("text"); // 翻译文本的格式
					eCommerceRequest.setSourceLanguage("zh"); // 源语言
					eCommerceRequest.setSourceText(URLEncoder.encode(sentence, "UTF-8")); // 原文
					eCommerceRequest.setTargetLanguage("ja"); // 目标语言
					TranslateECommerceResponse eCommerceResponse = client.getAcsResponse(eCommerceRequest);
					System.out.println(JSONObject.toJSON(eCommerceResponse));
					translatedSentence = eCommerceResponse.getData().getTranslated();
					System.out.println(translatedSentence);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return translatedSentence;
			}

			// 识别完毕
			@Override
			public void onTranscriptionComplete(SpeechTranscriberResponse response) {
				System.out.println("name: " + response.getName() + ", status: " + response.getStatus());
			}
		};
		return listener;
	}

	public void process() {
		SpeechTranscriber transcriber = null;
		try {
			// Step1 创建实例,建立连接
			transcriber = new SpeechTranscriber(client, getTranscriberListener());
			transcriber.setAppKey(appKey);
			// 输入音频编码方式
			transcriber.setFormat(InputFormatEnum.PCM);
			// 输入音频采样率
			transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
			// 是否返回中间识别结果
			transcriber.setEnableIntermediateResult(true);
			// 是否生成并返回标点符号
			transcriber.setEnablePunctuation(true);
			// 是否将返回结果规整化,比如将一百返回为100
			transcriber.setEnableITN(false);

			// Step2 此方法将以上参数设置序列化为json发送给服务端,并等待服务端确认
			transcriber.start();

			// Step3 读取麦克风数据
			AudioFormat audioFormat = new AudioFormat(16000.0F, 16, 1, true, false);
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
			TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
			targetDataLine.open(audioFormat);
			targetDataLine.start();
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					// Update UI here.
					labelOriginal.setText("你可以开始说话了!");
					labelTranslate.setText("話を始めてください!");
				}
			});
			// label.setText("You can speak now!");
			int nByte = 0;
			final int bufSize = 6400;
			byte[] buffer = new byte[bufSize];
			while ((nByte = targetDataLine.read(buffer, 0, bufSize)) > 0) {
				// Step4 直接发送麦克风数据流
				transcriber.send(buffer);
			}

			// Step5 通知服务端语音数据发送完毕,等待服务端处理完成
			transcriber.stop();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			// Step6 关闭连接
			if (null != transcriber) {
				transcriber.close();
			}
		}
	}

	// public void shutdown() {
	// client.shutdown();
	// }

	@Override
	public void run() {
		// Step0 创建NlsClient实例,应用全局创建一个即可,默认服务地址为阿里云线上服务地址
		AccessToken token = new AccessToken("your akID", "your akSecret");
		try {
			token.apply();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String accessToken = token.getToken();
		System.out.println(accessToken);// 1c6acc5765d34256973ac1239d0aebd2
		long expireTime = token.getExpireTime();
		client = new NlsClient(accessToken);
		this.process();
	}
}