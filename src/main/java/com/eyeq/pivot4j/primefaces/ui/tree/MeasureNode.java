package com.eyeq.pivot4j.primefaces.ui.tree;

import java.util.Collections;
import java.util.List;

import org.olap4j.metadata.Member;
import org.primefaces.model.TreeNode;

public class MeasureNode extends NavigatorNode<Member> {

	/**
	 * @param parent
	 * @param member
	 */
	public MeasureNode(TreeNode parent, Member member) {
		super(member);
		setParent(parent);
	}

	/**
	 * @see org.primefaces.model.TreeNode#getType()
	 */
	@Override
	public String getType() {
		return "measure";
	}

	/**
	 * @see org.primefaces.model.TreeNode#isLeaf()
	 */
	@Override
	public boolean isLeaf() {
		return true;
	}

	/**
	 * @see com.eyeq.pivot4j.primefaces.ui.tree.NavigatorNode#createChildren()
	 */
	@Override
	protected List<TreeNode> createChildren() {
		return Collections.emptyList();
	}
}
