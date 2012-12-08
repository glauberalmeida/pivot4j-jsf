package com.eyeq.pivot4j.ui.primefaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

import org.olap4j.Axis;
import org.olap4j.OlapException;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Dimension.Type;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.primefaces.event.DragDropEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.eyeq.pivot4j.ModelChangeEvent;
import com.eyeq.pivot4j.ModelChangeListener;
import com.eyeq.pivot4j.PivotModel;
import com.eyeq.pivot4j.transform.ChangeSlicer;
import com.eyeq.pivot4j.transform.PlaceHierarchiesOnAxes;
import com.eyeq.pivot4j.transform.PlaceLevelsOnAxes;
import com.eyeq.pivot4j.transform.PlaceMembersOnAxes;
import com.eyeq.pivot4j.ui.primefaces.tree.CubeNode;
import com.eyeq.pivot4j.ui.primefaces.tree.HierarchyNode;
import com.eyeq.pivot4j.ui.primefaces.tree.LevelNode;
import com.eyeq.pivot4j.ui.primefaces.tree.MemberNode;
import com.eyeq.pivot4j.ui.primefaces.tree.NodeSelectionFilter;

@ManagedBean(name = "navigatorHandler")
@RequestScoped
public class NavigatorHandler implements ModelChangeListener,
		NodeSelectionFilter {

	@ManagedProperty(value = "#{pivotModelManager.model}")
	private PivotModel model;

	private CubeNode cubeNode;

	private TreeNode targetNode;

	private List<Dimension> dimensions;

	private Map<Axis, List<Hierarchy>> hierarchies;

	private Map<Hierarchy, List<Level>> levels;

	private Map<Hierarchy, List<Member>> members;

	@PostConstruct
	protected void initialize() {
		model.addModelChangeListener(this);
	}

	@PreDestroy
	protected void destroy() {
		model.removeModelChangeListener(this);
	}

	/**
	 * @return the model
	 */
	public PivotModel getModel() {
		return model;
	}

	/**
	 * @param model
	 *            the model to set
	 */
	public void setModel(PivotModel model) {
		this.model = model;
	}

	/**
	 * @param axis
	 * @return
	 */
	protected List<Dimension> getDimensions(Axis axis) {
		if (dimensions == null) {
			this.dimensions = new ArrayList<Dimension>();

			for (Hierarchy hierarchy : getHierarchies(axis)) {
				dimensions.add(hierarchy.getDimension());
			}
		}

		return dimensions;
	}

	/**
	 * @param axis
	 * @return
	 */
	protected List<Hierarchy> getHierarchies(Axis axis) {
		if (hierarchies == null) {
			this.hierarchies = new HashMap<Axis, List<Hierarchy>>(2);
		}

		List<Hierarchy> result = hierarchies.get(axis);
		if (result == null) {
			if (axis.equals(Axis.FILTER)) {
				ChangeSlicer transform = model.getTransform(ChangeSlicer.class);
				result = transform.getHierarchies();
			} else {
				PlaceHierarchiesOnAxes transform = model
						.getTransform(PlaceHierarchiesOnAxes.class);
				result = transform.findVisibleHierarchies(axis);
			}

			hierarchies.put(axis, result);
		}

		return result;
	}

	/**
	 * @param hierarchy
	 * @return
	 */
	protected List<Level> getLevels(Hierarchy hierarchy) {
		if (levels == null) {
			this.levels = new HashMap<Hierarchy, List<Level>>();
		}

		List<Level> result = levels.get(hierarchy);
		if (result == null) {
			PlaceLevelsOnAxes transform = model
					.getTransform(PlaceLevelsOnAxes.class);

			result = transform.findVisibleLevels(hierarchy);
			levels.put(hierarchy, result);
		}

		return result;
	}

	/**
	 * @param hierarchy
	 * @return
	 */
	protected List<Member> getMembers(Hierarchy hierarchy) {
		if (members == null) {
			this.members = new HashMap<Hierarchy, List<Member>>();
		}

		List<Member> result = members.get(hierarchy);
		if (result == null) {
			PlaceMembersOnAxes transform = model
					.getTransform(PlaceMembersOnAxes.class);

			result = transform.findVisibleMembers(hierarchy);
			members.put(hierarchy, result);
		}

		return result;
	}

	/**
	 * @return the cubeNode
	 */
	public CubeNode getCubeNode() {
		if (model.isInitialized()) {
			if (cubeNode == null) {
				this.cubeNode = new CubeNode(model.getCube());
				cubeNode.setNodeFilter(this);
			}
		} else {
			this.cubeNode = null;
		}

		return cubeNode;
	}

	/**
	 * @param cubeNode
	 *            the cubeNode to set
	 */
	public void setCubeNode(CubeNode cubeNode) {
		this.cubeNode = cubeNode;

		this.dimensions = null;
		this.hierarchies = null;
		this.levels = null;
		this.members = null;
	}

	/**
	 * @return the cubeNode
	 */
	public TreeNode getTargetNode() {
		if (model.isInitialized()) {
			if (targetNode == null) {
				this.targetNode = new DefaultTreeNode();

				DefaultTreeNode columns = new DefaultTreeNode();
				columns.setExpanded(true);
				columns.setType("columns");
				columns.setData(Axis.COLUMNS);
				columns.setParent(targetNode);

				configureAxis(columns, Axis.COLUMNS);

				DefaultTreeNode rows = new DefaultTreeNode();
				rows.setExpanded(true);
				rows.setType("rows");
				rows.setData(Axis.ROWS);
				rows.setParent(targetNode);

				configureAxis(rows, Axis.ROWS);
			}
		} else {
			this.targetNode = null;
		}

		return targetNode;
	}

	/**
	 * @param targetNode
	 *            the targetNode to set
	 */
	public void setTargetNode(TreeNode targetNode) {
		this.targetNode = targetNode;
	}

	/**
	 * @param axisRoot
	 * @param axis
	 * @throws OlapException
	 */
	protected void configureAxis(TreeNode axisRoot, Axis axis) {
		List<Hierarchy> hierarchies = getHierarchies(axis);
		for (Hierarchy hierarchy : hierarchies) {
			DefaultTreeNode hierarchyNode = new DefaultTreeNode();
			hierarchyNode.setData(hierarchy);
			hierarchyNode.setType("hierarchy");
			hierarchyNode.setExpanded(true);
			hierarchyNode.setParent(axisRoot);

			Type type;
			try {
				type = hierarchy.getDimension().getDimensionType();
			} catch (OlapException e) {
				throw new FacesException(e);
			}

			if (type == Type.MEASURE) {
				List<Member> members = getMembers(hierarchy);
				for (Member member : members) {
					DefaultTreeNode memberNode = new DefaultTreeNode();
					memberNode.setData(member);
					memberNode.setType("member");
					memberNode.setParent(hierarchyNode);
				}
			} else {
				List<Level> levels = getLevels(hierarchy);
				for (Level level : levels) {
					DefaultTreeNode levelNode = new DefaultTreeNode();
					levelNode.setData(level);
					levelNode.setType("level");
					levelNode.setParent(hierarchyNode);
				}
			}
		}
	}

	/**
	 * @param id
	 * @return
	 */
	protected List<Integer> getNodePath(String id) {
		// there should be a cleaner way to get data from the dropped component.
		// it's a limitation on PFs' side :
		// http://code.google.com/p/primefaces/issues/detail?id=2781
		String[] segments = id.split(":");
		String[] indexSegments = segments[segments.length - 2].split("_");

		List<Integer> path = new ArrayList<Integer>(indexSegments.length);
		for (String index : indexSegments) {
			path.add(Integer.parseInt(index));
		}

		return path;
	}

	/**
	 * @param id
	 * @return
	 */
	protected boolean isSourceNode(String id) {
		return id.startsWith("source-tree-form:cube-navigator");
	}

	/**
	 * @param e
	 */
	public void onDrop(DragDropEvent e) {
		List<Integer> path = getNodePath(e.getDragId());

		boolean fromNavigator = isSourceNode(e.getDragId());
		if (fromNavigator) {
			return;
		}

		TreeNode node = findNodeFromPath(getTargetNode(), path);

		if (node.getData() instanceof Hierarchy) {
			Axis axis = (Axis) node.getParent().getData();
			Hierarchy hierarchy = (Hierarchy) node.getData();

			removeHierarhy(axis, hierarchy);
		} else if (node.getData() instanceof Level) {
			Axis axis = (Axis) node.getParent().getParent().getData();
			Level level = (Level) node.getData();

			removeLevel(axis, level);
		} else if (node.getData() instanceof Member) {
			Member member = (Member) node.getData();

			removeMember(member);
		}
	}

	/**
	 * @param e
	 */
	public void onDropOnAxis(DragDropEvent e) {
		List<Integer> sourcePath = getNodePath(e.getDragId());
		List<Integer> targetPath = getNodePath(e.getDropId());

		boolean fromNavigator = isSourceNode(e.getDragId());

		TreeNode rootNode = fromNavigator ? getCubeNode() : getTargetNode();

		TreeNode sourceNode = findNodeFromPath(rootNode, sourcePath);
		TreeNode targetNode = findNodeFromPath(getTargetNode(), targetPath);

		if (fromNavigator) {
			onDropOnAxis(sourceNode, targetNode);
		} else if (sourceNode.getData() instanceof Hierarchy) {
			Axis targetAxis = (Axis) targetNode.getData();
			Hierarchy hierarchy = (Hierarchy) sourceNode.getData();

			if (sourceNode.getParent().equals(targetNode)) {
				moveHierarhy(targetAxis, hierarchy, 0);
			} else {
				Axis sourceAxis = (Axis) sourceNode.getParent().getData();

				removeHierarhy(sourceAxis, hierarchy);
				addHierarhy(targetAxis, hierarchy);
			}
		}
	}

	/**
	 * @param sourceNode
	 * @param targetNode
	 */
	protected void onDropOnAxis(TreeNode sourceNode, TreeNode targetNode) {
		Axis axis = (Axis) targetNode.getData();

		if (sourceNode instanceof HierarchyNode) {
			HierarchyNode node = (HierarchyNode) sourceNode;
			Hierarchy hierarchy = node.getElement();

			addHierarhy(axis, hierarchy);
		} else if (sourceNode instanceof LevelNode) {
			LevelNode node = (LevelNode) sourceNode;
			Level level = node.getElement();

			addLevel(axis, level);
		} else if (sourceNode instanceof MemberNode) {
			MemberNode node = (MemberNode) sourceNode;
			Member member = node.getElement();

			addMember(axis, member);
		}
	}

	/**
	 * @param e
	 */
	public void onDropOnHierarchy(DragDropEvent e) {
		List<Integer> sourcePath = getNodePath(e.getDragId());
		List<Integer> targetPath = getNodePath(e.getDropId());

		int position = targetPath.get(targetPath.size() - 1) + 1;

		boolean fromNavigator = isSourceNode(e.getDragId());

		TreeNode rootNode = fromNavigator ? getCubeNode() : getTargetNode();

		TreeNode sourceNode = findNodeFromPath(rootNode, sourcePath);
		TreeNode targetNode = findNodeFromPath(getTargetNode(), targetPath);

		if (fromNavigator) {
			onDropOnHierarchy(sourceNode, targetNode, position);
		} else if (sourceNode.getData() instanceof Hierarchy) {
			Axis targetAxis = (Axis) targetNode.getParent().getData();
			Hierarchy hierarchy = (Hierarchy) sourceNode.getData();

			if (sourceNode.getParent().equals(targetNode)) {
				moveHierarhy(targetAxis, hierarchy, position);
			} else {
				Axis sourceAxis = (Axis) sourceNode.getParent().getData();

				removeHierarhy(sourceAxis, hierarchy);
				addHierarhy(targetAxis, hierarchy, position);
			}
		}
	}

	/**
	 * @param sourceNode
	 * @param targetNode
	 * @param position
	 */
	protected void onDropOnHierarchy(TreeNode sourceNode, TreeNode targetNode,
			int position) {
		Axis axis = (Axis) targetNode.getParent().getData();

		if (sourceNode instanceof HierarchyNode) {
			HierarchyNode node = (HierarchyNode) sourceNode;
			Hierarchy hierarchy = node.getElement();

			addHierarhy(axis, hierarchy, position);
		} else if (sourceNode instanceof LevelNode) {
			LevelNode node = (LevelNode) sourceNode;
			Level level = node.getElement();

			addLevel(axis, level, position);
		} else if (sourceNode instanceof MemberNode) {
			MemberNode node = (MemberNode) sourceNode;
			Member member = node.getElement();

			addMember(axis, member, position);
		}
	}

	/**
	 * @param axis
	 * @param hierarchy
	 */
	protected void addHierarhy(Axis axis, Hierarchy hierarchy) {
		addHierarhy(axis, hierarchy, 0);
	}

	/**
	 * @param axis
	 * @param hierarchy
	 * @param position
	 */
	protected void addHierarhy(Axis axis, Hierarchy hierarchy, int position) {
		for (Axis ax : new Axis[] { Axis.COLUMNS, Axis.ROWS, Axis.FILTER }) {
			List<Hierarchy> hiersInAxis = getHierarchies(ax);

			if (hiersInAxis.contains(hierarchy)) {
				String title = "Unable to add hierarchy.";
				String message = String
						.format("The selected hierarchy already exists in the '%s' axis.",
								ax.name());

				FacesContext context = FacesContext.getCurrentInstance();
				context.addMessage(null, new FacesMessage(
						FacesMessage.SEVERITY_WARN, title, message));
				return;
			}
		}

		PlaceHierarchiesOnAxes transform = getModel().getTransform(
				PlaceHierarchiesOnAxes.class);

		transform.addHierarchy(axis, hierarchy, false, position);
	}

	/**
	 * @param axis
	 * @param hierarchy
	 * @param position
	 */
	protected void moveHierarhy(Axis axis, Hierarchy hierarchy, int position) {
		PlaceHierarchiesOnAxes transform = getModel().getTransform(
				PlaceHierarchiesOnAxes.class);
		transform.moveHierarchy(axis, hierarchy, position);
	}

	/**
	 * @param axis
	 * @param hierarchy
	 */
	protected void removeHierarhy(Axis axis, Hierarchy hierarchy) {
		PlaceHierarchiesOnAxes transform = getModel().getTransform(
				PlaceHierarchiesOnAxes.class);
		transform.removeHierarchy(axis, hierarchy);
	}

	/**
	 * @param axis
	 * @param level
	 */
	protected void addLevel(Axis axis, Level level) {
		addLevel(axis, level, 0);
	}

	/**
	 * @param axis
	 * @param level
	 * @param position
	 */
	protected void addLevel(Axis axis, Level level, int position) {
		Hierarchy hierarchy = level.getHierarchy();

		for (Axis ax : new Axis[] { Axis.COLUMNS, Axis.ROWS, Axis.FILTER }) {
			if (ax.equals(axis)) {
				continue;
			}

			List<Hierarchy> hiersInAxis = getHierarchies(ax);

			if (hiersInAxis.contains(hierarchy)) {
				String title = "Unable to add level.";
				String message = String
						.format("Hierarchy of the selected level already exists in the '%s' axis.",
								ax.name());

				FacesContext context = FacesContext.getCurrentInstance();
				context.addMessage(null, new FacesMessage(
						FacesMessage.SEVERITY_WARN, title, message));
				return;
			}
		}

		PlaceLevelsOnAxes transform = getModel().getTransform(
				PlaceLevelsOnAxes.class);
		transform.addLevel(axis, level, position);
	}

	/**
	 * @param axis
	 * @param level
	 */
	protected void removeLevel(Axis axis, Level level) {
		PlaceLevelsOnAxes transform = getModel().getTransform(
				PlaceLevelsOnAxes.class);
		transform.removeLevel(axis, level);
	}

	/**
	 * @param axis
	 * @param member
	 */
	protected void addMember(Axis axis, Member member) {
		addMember(axis, member, 0);
	}

	/**
	 * @param axis
	 * @param member
	 * @param position
	 */
	protected void addMember(Axis axis, Member member, int position) {
		Hierarchy hierarchy = member.getHierarchy();

		for (Axis ax : new Axis[] { Axis.COLUMNS, Axis.ROWS, Axis.FILTER }) {
			if (ax.equals(axis)) {
				continue;
			}

			List<Hierarchy> hiersInAxis = getHierarchies(ax);

			if (hiersInAxis.contains(hierarchy)) {
				String title = "Unable to add member.";
				String message = String
						.format("Hierarchy of the selected member already exists in the '%s' axis.",
								ax.name());

				FacesContext context = FacesContext.getCurrentInstance();
				context.addMessage(null, new FacesMessage(
						FacesMessage.SEVERITY_WARN, title, message));
				return;
			}
		}

		PlaceMembersOnAxes transform = getModel().getTransform(
				PlaceMembersOnAxes.class);
		transform.addMember(axis, member, position);
	}

	/**
	 * @param member
	 */
	protected void removeMember(Member member) {
		PlaceMembersOnAxes transform = getModel().getTransform(
				PlaceMembersOnAxes.class);
		transform.removeMember(member);
	}

	/**
	 * @param parent
	 * @param indexes
	 * @return
	 */
	protected TreeNode findNodeFromPath(TreeNode parent, List<Integer> indexes) {
		if (indexes.size() > 1) {
			return findNodeFromPath(parent.getChildren().get(indexes.get(0)),
					indexes.subList(1, indexes.size()));
		} else {
			return parent.getChildren().get(indexes.get(0));
		}
	}

	/**
	 * @see com.eyeq.pivot4j.ui.primefaces.tree.NodeSelectionFilter#isSelected(org.olap4j.metadata.Dimension)
	 */
	@Override
	public boolean isSelected(Dimension dimension) {
		return getDimensions(Axis.COLUMNS).contains(dimension)
				|| getDimensions(Axis.ROWS).contains(dimension);
	}

	/**
	 * @see com.eyeq.pivot4j.ui.primefaces.tree.NodeSelectionFilter#isSelected(org.olap4j.metadata.Hierarchy)
	 */
	@Override
	public boolean isSelected(Hierarchy hierarchy) {
		return getHierarchies(Axis.COLUMNS).contains(hierarchy)
				|| getHierarchies(Axis.ROWS).contains(hierarchy);
	}

	/**
	 * @see com.eyeq.pivot4j.ui.primefaces.tree.NodeSelectionFilter#isSelected(org.olap4j.metadata.Level)
	 */
	@Override
	public boolean isSelected(Level level) {
		return getLevels(level.getHierarchy()).contains(level);
	}

	/**
	 * @see com.eyeq.pivot4j.ui.primefaces.tree.NodeSelectionFilter#isSelected(org.olap4j.metadata.Member)
	 */
	@Override
	public boolean isSelected(Member member) {
		return getMembers(member.getHierarchy()).contains(member);
	}

	/**
	 * @see com.eyeq.pivot4j.ModelChangeListener#modelInitialized(com.eyeq.pivot4j.ModelChangeEvent)
	 */
	@Override
	public void modelInitialized(ModelChangeEvent e) {
	}

	/**
	 * @see com.eyeq.pivot4j.ModelChangeListener#modelDestroyed(com.eyeq.pivot4j.ModelChangeEvent)
	 */
	@Override
	public void modelDestroyed(ModelChangeEvent e) {
	}

	/**
	 * @see com.eyeq.pivot4j.ModelChangeListener#modelChanged(com.eyeq.pivot4j.ModelChangeEvent)
	 */
	@Override
	public void modelChanged(ModelChangeEvent e) {
	}

	/**
	 * @see com.eyeq.pivot4j.ModelChangeListener#structureChanged(com.eyeq.pivot4j.ModelChangeEvent)
	 */
	@Override
	public void structureChanged(ModelChangeEvent e) {
		this.cubeNode = null;
		this.targetNode = null;

		this.hierarchies = null;
		this.levels = null;
		this.members = null;
	}
}