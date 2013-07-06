package com.liyun;

import org.junit.Assert;
import org.junit.Test;

import com.dakele.cloud.MyLCS;

public class MyLCSTest {

	@Test
	public void testLCS1() {
		MyLCS m = new MyLCS("zhang chang zhi", "zhcz");
		Assert.assertTrue(m.check());
		m.printMatch();

		m = new MyLCS("zhang chang zhi", "zhcg");
		Assert.assertTrue(!m.check());
		m.printMatch();

		m = new MyLCS("zhang chang zhi", "zhgz");
		Assert.assertTrue(!m.check());
		m.printMatch();

		m = new MyLCS("zhang chang zhi", "zhch");
		Assert.assertTrue(m.check());
		m.printMatch();
		
		m = new MyLCS("zhang chang zhi", "zhz");
		Assert.assertTrue(!m.check());
		m.printMatch();
		
		m = new MyLCS("zhang hang zhi", "zhz");
		Assert.assertTrue(m.check());
		m.printMatch();
	}
}
