/* Modified from the upstream Ansj Seg source by LinguaMesh maintainers in 2026. */

package org.ansj.app.crf.model;

import org.ansj.app.crf.Config;
import org.ansj.app.crf.Model;
import org.nlpcn.commons.lang.tire.domain.SmartForest;
import org.nlpcn.commons.lang.util.IOUtil;
import org.nlpcn.commons.lang.util.ObjConver;
import org.nlpcn.commons.lang.util.StringUtil;
import org.nlpcn.commons.lang.util.tuples.Pair;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 加载CRF+生成的crf文本模型,测试使用的CRF++版本为:CRF++-0.58
 * 
 * 下载地址:https://taku910.github.io/crfpp/#download 在这里感谢作者所做的工作.
 * 
 * @author Ansj
 *
 */
public class CRFppTxtModel extends Model {

	/**
	 * 解析crf++生成的可可视txt文件
	 * 
	 * @return
	 */
	@Override
	public CRFppTxtModel loadModel(String modelPath) throws Exception {
		try (InputStream is = new FileInputStream(modelPath)) {
			loadModel(new FileInputStream(modelPath));
			return this;
		}
	}

	@Override
	public Model loadModel(InputStream is) throws Exception {
		long start = System.currentTimeMillis();

		BufferedReader reader = IOUtil.getReader(is, IOUtil.UTF8);

		reader.readLine();// version
		reader.readLine();// cost-factor

		// int maxId =
		// Integer.parseInt(reader.readLine().split(":")[1].trim());// read
		reader.readLine();// xsize
		reader.readLine(); // line
		int[] statusCoven = loadTagCoven(reader);
		Map<String, Integer> featureIndex = loadConfig(reader);
		StringBuilder sb = new StringBuilder();
		for (int[] t1 : config.getTemplate()) {
			sb.append(Arrays.toString(t1) + " ");
		}
		logger.info("load template ok template : " + sb);
		TreeMap<Integer, Pair<String, String>> featureNames = loadFeatureName(featureIndex, reader);
		logger.info("load feature ok feature size : " + featureNames.size());
		loadFeatureWeight(reader, statusCoven, featureNames);
		logger.info("load crfpp model ok ! use time : " + (System.currentTimeMillis() - start));
		return this;
	}

	/**
	 * 加载特征值 //11:*6:_x-1/的,
	 * 
	 * @param maxId
	 * 
	 * @param featureIndex
	 * 
	 * @param br
	 * @return
	 * @throws Exception
	 */

	private TreeMap<Integer, Pair<String, String>> loadFeatureName(Map<String, Integer> featureIndex, BufferedReader br) throws Exception {

		TreeMap<Integer, Pair<String, String>> featureNames = new TreeMap<Integer, Pair<String, String>>();

		String temp = null;
		while (StringUtil.isNotBlank(temp = br.readLine())) {

			int indexOf = temp.indexOf(" ");

			int id = ObjConver.getIntValue(temp.substring(0, indexOf));

			if (indexOf > 0) {
				temp = temp.substring(indexOf);
			}

			String[] split = temp.split(":");

			if (split.length == 1) {
				featureNames.put(id, Pair.with(temp.trim(), ""));
			} else {
				String name = split[1];
				if (split.length > 2) {
					for (int j = 2; j < split.length; j++) {
						name += ":" + split[j];
					}
				}

				int lastFeatureId = featureIndex.get(split[0].trim());

				if ("/".equals(name)) {
					name = "//";
				}

				if (name.contains("//")) {
					name = name.replaceAll("//", "/XIEGANG/");
				}
				String featureName = toFeatureName(name.trim().split("/"), lastFeatureId);

				featureNames.put(id, Pair.with(split[0].trim(), featureName));

			}

		}

		return featureNames;

	}

	private String toFeatureName(String[] split, int lastFeatureId) throws Exception {

		StringBuilder result = new StringBuilder();

		for (String str : split) {
			if ("".equals(str)) {
				continue;
			} else if (str.length() == 1) {
				result.append(str.charAt(0));
			} else if (str.equals("XIEGANG")) {
				result.append('/');
			} else if (str.startsWith("num")) {
				result.append((char) (Config.NUM_BEGIN + ObjConver.getIntValue(str.replace("num", ""))));
			} else if (str.startsWith("en")) {
				result.append((char) (Config.EN_BEGIN + ObjConver.getIntValue(str.replace("en", ""))));
			} else if (str.startsWith("_B-")) {
				result.append(Config.BEGIN);
			} else if (str.startsWith("_B+")) {
				result.append(Config.END);
			} else {
				throw new Exception("can find feature named " + str + " in " + Arrays.toString(split));
			}
		}

		result.append((char) (lastFeatureId + Config.FEATURE_BEGIN));

		return result.toString();
	}

	/**
	 * 加载特征权重
	 * 
	 * @param br
	 * @param featureNames
	 * @param statusCoven
	 * @throws Exception
	 */
	private void loadFeatureWeight(BufferedReader br, int[] statusCoven, TreeMap<Integer, Pair<String, String>> featureNames) throws Exception {

		featureTree = new SmartForest<float[]>();

		int tag = 0; // 赏析按标签为用来转换

		int len = 0; // 权重数组的大小

		String name = null; // 特征名称

		float[] tempW = null; // 每一个特征的权重

		String temp = null;

		for (Pair<String, String> pair : featureNames.values()) {

			char fc = Character.toUpperCase(pair.getValue0().charAt(0));

			len = fc == 'B' ? Config.TAG_NUM * Config.TAG_NUM : fc == 'U' ? Config.TAG_NUM : fc == '*' ? (Config.TAG_NUM + Config.TAG_NUM * Config.TAG_NUM) : 0;

			if (len == 0) {
				throw new Exception("unknow feature type " + pair.getValue0());
			}

			if (fc == 'B') { // 特殊处理转换特征数组
				for (int i = 0; i < len; i++) {
					temp = br.readLine();
					int from = statusCoven[i / Config.TAG_NUM];
					int to = statusCoven[i % Config.TAG_NUM];
					status[from][to] = ObjConver.getFloatValue(temp);
				}

			} else {

				name = pair.getValue1();

				tempW = new float[len];

				for (int i = 0; i < len; i++) {
					temp = br.readLine();
					tag = statusCoven[i];
					tempW[tag] = ObjConver.getFloatValue(temp);
				}
				this.featureTree.add(name, tempW); // 将特征增加到特征🌲中

				// printFeatureTree(name, tempW);
			}

		}

	}

	/**
	 * 加载特征标签转换
	 * 
	 * @param br
	 * @return
	 * @throws Exception
	 */
	private int[] loadTagCoven(BufferedReader br) throws Exception {

		int[] conver = new int[Config.TAG_NUM + Config.TAG_NUM * Config.TAG_NUM];

		String temp = null;

		// CRF++ model files use the fixed S/B/M/E tag set.
		for (int i = 0; i < Config.TAG_NUM; i++) {
			String line = br.readLine();
			if (StringUtil.isBlank(line)) {
				i--;
				continue;
			}

			char c = line.charAt(0);
			switch (c) {
			case 'S':
				conver[i] = Config.S;
				break;
			case 'B':
				conver[i] = Config.B;
				break;
			case 'M':
				conver[i] = Config.M;
				break;
			case 'E':
				conver[i] = Config.E;
				break;
			default:
				throw new Exception("err tag named " + c + " in model " + temp);
			}
		}

		for (int i = Config.TAG_NUM; i < conver.length; i++) {
			conver[i] = conver[(i - Config.TAG_NUM) / Config.TAG_NUM] * Config.TAG_NUM + conver[i % Config.TAG_NUM] + Config.TAG_NUM;
		}

		return conver;
	}

	private Map<String, Integer> loadConfig(BufferedReader br) throws IOException {

		Map<String, Integer> featureIndex = new HashMap<String, Integer>();

		String temp = br.readLine();// #rdr#8/0/0

		List<int[]> list = new ArrayList<int[]>();

		while (StringUtil.isNotBlank((temp = br.readLine()))) {

			List<String> matcherAll = StringUtil.matcherAll("\\[.*?\\]", temp);

			if (matcherAll.size() == 0) {
				continue;
			}

			int[] is = new int[matcherAll.size()];
			for (int j = 0; j < is.length; j++) {
				is[j] = ObjConver.getIntValue(StringUtil.matcherFirst("[-\\d]+", matcherAll.get(j)));
			}

			featureIndex.put(temp.split(":")[0].trim(), list.size());

			list.add(is);
		}

		int[][] template = new int[list.size()][0]; // 构建特征模板

		for (int i = 0; i < template.length; i++) {
			template[i] = list.get(i);
		}

		config = new Config(template);

		return featureIndex;
	}

	@Override
	public boolean checkModel(String modelPath) {

		try (InputStream is = IOUtil.getInputStream(modelPath)) {
			byte[] bytes = new byte[100];
			is.read(bytes);
			String string = new String(bytes);
			if (string.startsWith("version")) { // 加载crf++ 的txt类型的modle
				return true;
			}
		} catch (IOException e) {
			logger.warn("IO异常", e);
		}
		return false;
	}

}
