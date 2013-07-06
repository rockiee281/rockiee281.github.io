package com.dakele.cloud;

public class MyLCS {
	private String base;

	private String target;
	private String[] baseWords;
	private int[] matchPositions;
	private char[] targetChar;
	private char[][] baseCharArray;

	public MyLCS(String base, String target) {
		this.base = base;
		this.target = target;
		this.baseWords = base.split(" ");
		matchPositions = new int[baseWords.length];
		targetChar = target.toCharArray();
		baseCharArray = new char[baseWords.length][];
		for (int i = 0; i < baseWords.length; i++) {
			baseCharArray[i] = baseWords[i].toCharArray();
		}
	}

	public boolean check() {

		for (int i = 0; i < baseWords.length; i++) {
			if (checkSub(i)) {
				return true;
			}
			matchPositions = new int[baseWords.length];
		}

		return false;
	}

	public void printMatch() {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < baseWords.length; i++) {
			if (matchPositions[i] > 0) {
				sb.append("<m>" + baseWords[i] + "</m> ");
			} else {
				sb.append(baseWords[i] + " ");
			}
		}
		System.out.println(target + "|" + sb);
	}

	private boolean checkSub(int startBaseWordIndex) {

		// System.out.println("target is [" + target + "], start from base in ["
		// + startBIndex + "]");
		int tIndex = 0;
		for (int wordIndex = startBaseWordIndex; wordIndex < baseWords.length; wordIndex++) {

			char[] baseCharOfWord = baseCharArray[wordIndex];
			for (int charIndex = 0; charIndex < baseCharOfWord.length; charIndex++) {
				if (baseCharOfWord[charIndex] == targetChar[tIndex]) {
					tIndex++;
					matchPositions[wordIndex] = 1;
					if (tIndex == targetChar.length) { // match all the target
														// char
						return true;
					}
				} else {
					if (startBaseWordIndex == baseWords.length - 1
							|| baseCharArray[startBaseWordIndex + 1][0] != targetChar[tIndex]) {// 如果已经到了最后一个词或者下一个单词的首字母和当前并不匹配

						if (tIndex > 0 && targetChar[tIndex -1 ] == baseCharArray[wordIndex][0]) {
							matchPositions[wordIndex] = 1;
							/**
							 * 增加后向纠错,因为类似h这种字母，既可以作为前一个单词的声母组成部分，也可以作为本单词的声母
							 * 
							 * */ 
							break;
						}

					} else {
						break; // match failed， skip to next word
					}

				}
			}
		}
		return false;
	}

	public static void main(String[] args) {
		MyLCS m = new MyLCS("zhang chang zhi", "zhcz");
		System.out.println(m.check());
		m.printMatch();

		m = new MyLCS("zhang chang zhi", "zhcg");
		System.out.println(m.check());
		m.printMatch();

		m = new MyLCS("zhang chang zhi", "zhgz");
		System.out.println(m.check());
		m.printMatch();

		System.out.println(lcs("zhangchangzhi", "hcz"));
	}

	public static String lcs(String a, String b) {
		int aLen = a.length();
		int bLen = b.length();
		if (aLen == 0 || bLen == 0) {
			return "";
		} else if (a.charAt(aLen - 1) == b.charAt(bLen - 1)) {
			return lcs(a.substring(0, aLen - 1), b.substring(0, bLen - 1)) + a.charAt(aLen - 1);
		} else {
			String x = lcs(a, b.substring(0, bLen - 1));
			String y = lcs(a.substring(0, aLen - 1), b);
			return (x.length() > y.length()) ? x : y;
		}
	}
}
