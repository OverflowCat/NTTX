package io.kurumi.telegraph.model;

import java.util.Map;
import java.util.List;

public class NodeElement extends Node {
		
	public transient NodeElement superNode;
	
		/*
		
		Name of the DOM element. Available tags: a, aside, b, blockquote, br, code, em, figcaption, figure, h3, h4, hr, i, iframe, img, li, ol, p, pre, s, strong, u, ul, video.
		
		*/
		
		public String tag;
		
		/*
		
		Optional. Attributes of the DOM element. Key of object represents name of attribute, value represents value of attribute. Available attributes: href, src.
		
		
		*/
		
		public Map<String,String> attrs;
		
		/*
		
		Optional. List of child nodes for the DOM element.
		
		*/
		
		public List<Node> children;
		
}
