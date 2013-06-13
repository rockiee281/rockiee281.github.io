---
layout: post
category : android
tagline: "android cloud tag"
tags : [android]
---
今天在讨论中大家想在我们的搜索页将搜索热词用wordpress中常见的tag云的形式来展示，功能不足噱头补嘛 XD  
然后就在google上搜索了一番。在中文的开发论坛里找到了一个[demo][1]，类似星空图的样子，效果不错。不过惊喜在后台，google上又找到了一个开源的项目[tagin!][2]，它实现了一个真正的浑天仪tag云，而且效果很华丽哟。代码地址在[这里][3]，感兴趣的可以下载体验一下。不过现在它是通过在屏幕边缘长按来控制tag球转动，我想把代码修改一下，让他可以在滑动的时候也能很方便的让tag云跟随转动。如果修改成功，稍后奉上代码。

下午大概的看了一下cloudtag imp4的代码，很简洁，核心的功能实现在一个TagCloudView中。其中，响应用户操作的代码，放在了view的`public boolean onTouchEvent(MotionEvent e)`方法中。原来作者的代码如下：

``` java
	public boolean onTouchEvent(MotionEvent e) {
		float x = e.getX();
		float y = e.getY();
//		Log.d(TAG, "motion:x=["+x+"] y=["+y+"] action is ["+e.getAction()+"]");
		switch (e.getAction()) {
		case MotionEvent.ACTION_MOVE:	
			//rotate elements depending on how far the selection point is from center of cloud
			float dx = x - centerX;
			float dy = y - centerY;
			
			Log.d(TAG, "motion:dx=["+dx+"] dy=["+dy+"] action is ["+e.getAction()+"]");
			
			mAngleX = ( dy/radius) *tspeed * TOUCH_SCALE_FACTOR;
			mAngleY = (-dx/radius) *tspeed * TOUCH_SCALE_FACTOR;
	    	
			mTagCloud.setAngleX(mAngleX);
	    	mTagCloud.setAngleY(mAngleY);
	    	mTagCloud.update();
	    	
	    	Iterator it=mTagCloud.iterator();
	    	Tag tempTag;
	    	while (it.hasNext()){
	    		tempTag= (Tag) it.next();              
	    		mParams.get(tempTag.getParamNo()).setMargins(	
						(int) (centerX -shiftLeft + tempTag.getLoc2DX()), 
						(int) (centerY + tempTag.getLoc2DY()), 
						0, 
						0);
				mTextView.get(tempTag.getParamNo()).setTextSize((int)(tempTag.getTextSize() * tempTag.getScale()));
				int mergedColor = Color.argb( (int)	(tempTag.getAlpha() * 255), 
						  (int)	(tempTag.getColorR() * 255), 
						  (int)	(tempTag.getColorG() * 255), 
						  (int) (tempTag.getColorB() * 255));
				mTextView.get(tempTag.getParamNo()).setTextColor(mergedColor);
				mTextView.get(tempTag.getParamNo()).bringToFront();
	    	}
			
			break;
		/*case MotionEvent.ACTION_UP:  //now it is clicked!!!!		
			dx = x - centerX;
			dy = y - centerY;			
			break;*/
		}
		
		return true;
	}
```
我做了一些小的修改，让不再是依靠长按屏幕某个位置来触发球体滚动，因为我觉得在手机上通过滑动来控制屏幕的变化更加自然，就改成了下面这个样子：


``` java
@Override
	public boolean onTouchEvent(MotionEvent e) {
		float x = e.getX();
		float y = e.getY();
//		Log.d(TAG, "motion:x=["+x+"] y=["+y+"] action is ["+e.getAction()+"]");
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			oldX = x;
			oldY = y;
			Log.d(TAG, "motion:x=["+x+"] y=["+y+"] action is ["+e.getAction()+"]");
			break;
		case MotionEvent.ACTION_MOVE:	
			//rotate elements depending on how far the selection point is from center of cloud
			float dx = x - oldX;
			float dy = y - oldY;
			oldX = x;
			oldY = y;
			
			Log.d(TAG, "motion:dx=["+dx+"] dy=["+dy+"] action is ["+e.getAction()+"]");
			
			mAngleX = ( dy/radius) *tspeed * TOUCH_SCALE_FACTOR;
			mAngleY = (-dx/radius) *tspeed * TOUCH_SCALE_FACTOR;
	    	
			mTagCloud.setAngleX(mAngleX);
	    	mTagCloud.setAngleY(mAngleY);
	    	mTagCloud.update();
	    	
	    	Iterator it=mTagCloud.iterator();
	    	Tag tempTag;
	    	while (it.hasNext()){
	    		tempTag= (Tag) it.next();              
	    		mParams.get(tempTag.getParamNo()).setMargins(	
						(int) (centerX -shiftLeft + tempTag.getLoc2DX()), 
						(int) (centerY + tempTag.getLoc2DY()), 
						0, 
						0);
				mTextView.get(tempTag.getParamNo()).setTextSize((int)(tempTag.getTextSize() * tempTag.getScale()));
				int mergedColor = Color.argb( (int)	(tempTag.getAlpha() * 255), 
						  (int)	(tempTag.getColorR() * 255), 
						  (int)	(tempTag.getColorG() * 255), 
						  (int) (tempTag.getColorB() * 255));
				mTextView.get(tempTag.getParamNo()).setTextColor(mergedColor);
				mTextView.get(tempTag.getParamNo()).bringToFront();
	    	}
			
			break;
		/*case MotionEvent.ACTION_UP:  //now it is clicked!!!!		
			dx = x - centerX;
			dy = y - centerY;			
			break;*/
		}
		
		return true;
	}
```



[1]: http://www.eyeandroid.com/thread-1313-1-1.html
[2]: https://sites.google.com/site/tagindemo/TagCloud
[3]: https://code.launchpad.net/~saranasr83/tagin/TagCloud