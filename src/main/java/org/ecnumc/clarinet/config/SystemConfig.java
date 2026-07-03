package org.ecnumc.clarinet.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ecnumc.clarinet.ClarinetLogger;
import org.ecnumc.clarinet.exception.FailedToLoadConfigException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 系统配置
 *
 * @author liudongyu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {
	/** 模型协议 */
	private String modelProtocol;
	/** 模型名称 */
	private String modelName;

	/** api key */
	private String apiKey;
	/** 模型服务地址 */
	private String baseUrl;

	/**
	 * 解析系统配置文件
	 * @param path 系统配置文件位置
	 * @return 系统配置
	 */
	public static SystemConfig parse(Path path) {
		File file = path.toFile();
		if(!file.exists()) {
			SystemConfig ret = new SystemConfig(
					"openai",
					"deepseek-v4-flash",
					"sk-kfccrazythursdayvivo50",
					"https://api.deepseek.com"
			);
			try {
				boolean success = file.createNewFile();
				assert success;
			} catch (IOException e) {
				ClarinetLogger.warn("An I/O error occurred when creating empty config: ", e);
				return ret;
			}
			try(OutputStream os = Files.newOutputStream(path)) {
				os.write(JSON.toJSONString(ret).getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				ClarinetLogger.warn("An I/O error occurred when saving system config: ", e);
			}
			return ret;
		}
		try (InputStream is = Files.newInputStream(path)) {
			return JSON.parseObject(is, SystemConfig.class);
		} catch (IOException e) {
			throw new FailedToLoadConfigException("An I/O error occurred when loading system config.", e);
		} catch (JSONException e) {
			throw new FailedToLoadConfigException("Failed to parse system config.", e);
		}
	}
}
