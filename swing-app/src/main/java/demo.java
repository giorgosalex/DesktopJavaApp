import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JTextField;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gcube.common.authorization.client.exceptions.ObjectNotFound;
import org.gcube.common.homelibrary.home.Home;
import org.gcube.common.homelibrary.home.HomeLibrary;
import org.gcube.common.homelibrary.home.HomeManager;
import org.gcube.common.homelibrary.home.HomeManagerFactory;
import org.gcube.common.homelibrary.home.User;
import org.gcube.common.homelibrary.home.exceptions.InternalErrorException;
import org.gcube.common.homelibrary.home.workspace.Workspace;
import org.gcube.common.homelibrary.home.workspace.WorkspaceFolder;
import org.gcube.common.homelibrary.home.workspace.WorkspaceItem;
import org.gcube.common.homelibrary.home.workspace.exceptions.ItemNotFoundException;
import org.gcube.common.homelibrary.home.workspace.folder.items.ExternalFile;
import org.gcube.common.scope.api.ScopeProvider;
import static org.gcube.common.authorization.client.Constants.authorizationService;

import javax.swing.JComboBox;
import javax.swing.JTextArea;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.awt.event.ActionEvent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeExpansionEvent;

public class demo {

	private JFrame frame;
	
	Workspace ws;
	WorkspaceFolder root;
	Map<String, WorkspaceItem> workspaceMap;
	Map<WorkspaceItem, List<WorkspaceItem>> workspaceChildrenMap;
	
	JPanel panel;
	
	JButton btnNewButton, downloadBtn, idsButton, writeSyncBtn, readSyncBtn;
	
	JTree tree2;
	
	Map<String, DefaultMutableTreeNode> mytree;
	
	Map<String, Calendar> syncFile;

	
	///////////////////////////		Launch the application.		//////////////////////////////////////
	public static void main(String[] args) {
		ScopeProvider.instance.set("/gcube/devsec/devVRE");
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					demo window = new demo();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	
	///////////////////////////		Create the application.		/////////////////////////////////////////
	public demo() {
		initialize();
	}

	
	///////////////////////////		Initialize the contents of the frame.		////////////////////////
	private void initialize() {
		frame = new JFrame();
		frame.setSize(400,400);
		
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension dim =  tk.getScreenSize();
		
		int xPos = (dim.width / 2) - (frame.getWidth());
		int yPos = (dim.height / 2) - (frame.getHeight());
		
		frame.setLocation(xPos, yPos);
		
		//frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		panel =  new JPanel();
		frame.getContentPane().add(panel);
		
		
		////////////////////////	BUTTONS CREATION AND LISTENERS ADDITION		//////////////////////////
		ButtonListener btnListener = new ButtonListener();
		
		btnNewButton = new JButton("New button");
		btnNewButton.addActionListener(btnListener);
		panel.add(btnNewButton);
		
		downloadBtn = new JButton("Download");
		downloadBtn.addActionListener(btnListener);
		panel.add(downloadBtn);
		//btnNewButton.setBounds(121, 214, 117, 25);
		
		idsButton = new JButton("Get ID's");
		idsButton.addActionListener(btnListener);
		panel.add(idsButton);
		
		writeSyncBtn = new JButton("save Sync");
		writeSyncBtn.addActionListener(btnListener);
		panel.add(writeSyncBtn);
		
		readSyncBtn = new JButton("read Sync");
		readSyncBtn.addActionListener(btnListener);
		panel.add(readSyncBtn);
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		syncFile = new HashMap<String, Calendar>();
		
		workspaceMap = new HashMap<String, WorkspaceItem>();
		workspaceChildrenMap =  new HashMap<WorkspaceItem, List<WorkspaceItem>>();
		   
        mytree = new HashMap<String, DefaultMutableTreeNode>();
        DefaultMutableTreeNode root2 = new DefaultMutableTreeNode("Workspace");
        mytree.put("/Workspace", root2);
        tree2 = new JTree(root2);
        tree2.setVisibleRowCount(8);
        
        ExpansionListener expListener = new ExpansionListener();
        tree2.addTreeExpansionListener(expListener);
        
        //ListenForMouse treeListener2 = new ListenForMouse();
		//tree2.addMouseListener(treeListener2);
        
        
		JScrollPane scrollBox = new JScrollPane(tree2);
		Dimension d = scrollBox.getPreferredSize();
        d.width = 200;
        scrollBox.setPreferredSize(d);
		panel.add(scrollBox);

	}
	
	private void DownloadContent(WorkspaceItem item, String path) throws InternalErrorException, IOException {
		if(item.isFolder()) {
			List<WorkspaceItem> children;
			if(workspaceChildrenMap.containsKey(item)) {
				children = workspaceChildrenMap.get(item);
			}else {
				WorkspaceFolder folder = (WorkspaceFolder) item;
				children = folder.getChildren();
				
				workspaceChildrenMap.put(item, children);
			}
			
			for(WorkspaceItem child:children){
				String name = child.getName();
				String childPath = path + "/" + name;
				if(!mytree.containsKey(childPath)) {
					mytree.put(childPath, new DefaultMutableTreeNode(name));
					mytree.get(path).add(mytree.get(childPath));
				}
				if(!workspaceMap.containsKey(childPath)) {
					workspaceMap.put(childPath, child);
				}
				
				DownloadContent(child, childPath);
			}
		}else {
			ExternalFile file = (ExternalFile) item;
			InputStream stream = file.getData();
			
			File targetFile = new File("/home/george/Desktop/myworkspace/" + path);
			FileUtils.copyInputStreamToFile(stream, targetFile);
			
			/////////////////////////	ADD ITEM TO SYNC FILE	/////////////////////////
			String itemID = item.getId();
			if(!syncFile.containsKey(itemID)) {
				syncFile.put(itemID, item.getLastModificationTime());
			}else {
				Calendar date1 = syncFile.get(itemID);
				Calendar date2 = item.getLastModificationTime();
				if(date1.compareTo(date2) < 0) {
					syncFile.put(itemID, date2);
				}
			}
		}
	}
	
	private void writeSyncFile(Map<String, Calendar> syncFile) throws IOException {
		FileOutputStream fileOut = new FileOutputStream("/home/george/Desktop/myworkspace/syncFile.bin");
		ObjectOutputStream out = new ObjectOutputStream(fileOut);
		out.writeObject(syncFile);
		out.close();
		fileOut.close();
	}
	
	private Map<String, Calendar> readSyncFile() throws IOException, ClassNotFoundException {
		Map<String, Calendar> syncFile;
		FileInputStream fileIn = new FileInputStream("/home/george/Desktop/myworkspace/syncFile.bin");
        ObjectInputStream in = new ObjectInputStream(fileIn);
        syncFile = (Map<String, Calendar>) in.readObject();
        in.close();
        fileIn.close();
        
        return syncFile;
	}
	
	private String getItemsPath(DefaultMutableTreeNode item) {
		TreeNode[] pathN = item.getPath();
		String path = "";
        for(TreeNode indivNodes: pathN){
        	path += "/" + indivNodes;
        }
        
        return path;
	}
	
	
	///////////////////////		LISTENERS IMPLEMENTATION	//////////////////////////////////
	
	private class ButtonListener implements ActionListener{

		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == downloadBtn) {
				Object treeObject = tree2.getLastSelectedPathComponent();
				// Cast the Object into a DefaultMutableTreeNode
				DefaultMutableTreeNode file = (DefaultMutableTreeNode) treeObject;
				
				String path = getItemsPath(file);
	            
	            System.out.println(path);
	            
	            
				try {
					WorkspaceItem item = ws.getItemByPath(path);
					
					DownloadContent(item, path);
				} catch (ItemNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InternalErrorException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			if(e.getSource() == idsButton) {
				for (Map.Entry<String, WorkspaceItem> entry : workspaceMap.entrySet())
				{
				    try {
						System.out.println(entry.getKey() + " : " + entry.getValue().getId());
						syncFile.put(entry.getValue().getId(), Calendar.getInstance());
					} catch (InternalErrorException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				
				///////////////		CREATE NEW THREAD	/////////////////////////////
				Thread thread1 = new Thread(new HelloRunnable());
				//thread1.setDaemon(true);
				thread1.start();
				//(new Thread(new HelloRunnable())).start();
			}
			
			if(e.getSource() == writeSyncBtn) {
				try {
					writeSyncFile(syncFile);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			if(e.getSource() == readSyncBtn) {
				Map<String, Calendar> newSync;
				try {
					newSync = readSyncFile();
					
					for (Map.Entry<String, Calendar> entry : newSync.entrySet()) {
						System.out.println(entry.getKey() + " : " + entry.getValue());
					}
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			if(e.getSource() == btnNewButton) {
				try {
					HomeManagerFactory factory = HomeLibrary.getHomeManagerFactory();
					HomeManager manager = factory.getHomeManager();
					
					String username =  authorizationService().get("994e41a4-01fb-4c6b-b953-24d02c8a5949-843339462").getClientInfo().getId();
					System.out.println(username);
					User user = manager.createUser(username);
					@SuppressWarnings("deprecation")
					Home home = manager.getHome(user);
					
					ws = home.getWorkspace();
					root = ws.getRoot();
					
					
					
					String path = root.getPath();
					System.out.println(path);
					WorkspaceItem item = ws.getItemByPath(path);
					workspaceMap.put(path, item);
					
					WorkspaceFolder folder = (WorkspaceFolder) item;
					List<WorkspaceItem> children = folder.getChildren();
					for(WorkspaceItem child:children){
						//System.out.println(child.getName());
						String name = child.getName();
						String path2 = path + "/" + name;
						
						mytree.put(path2, new DefaultMutableTreeNode(name));
						mytree.get(path).add(mytree.get(path2));
						
						workspaceMap.put(path2, child);
					}
					
					
					String vrePath = ws.getMySpecialFolders().getPath();
					System.out.println(vrePath);
					
					path += "/MySpecialFolders";	//////////////////	See if "VRE Folders" OR "MySpecialFolders" is required
					WorkspaceFolder folder2 = ws.getMySpecialFolders();
					List<WorkspaceItem> specialFolders = folder2.getChildren();
					for (WorkspaceItem vreFolder : specialFolders){
						String name = vreFolder.getName();
						String path2 = path + "/" + name;
						
						mytree.put(path2, new DefaultMutableTreeNode(name));
						mytree.get(path).add(mytree.get(path2));
						
						workspaceMap.put(path2, vreFolder);
						
					}
				} catch (InternalErrorException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (ObjectNotFound e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				System.out.println("Yup");
			}
		}
		
	}
	
	
	private class ExpansionListener implements TreeExpansionListener{

		public void treeExpanded(TreeExpansionEvent event) {
			System.out.println("EXPANDED");
    		
    		Object treeObject = event.getPath().getLastPathComponent();
    		DefaultMutableTreeNode file = (DefaultMutableTreeNode) treeObject;
    		
    		String path = getItemsPath(file);
            
            System.out.println(path);
            System.out.println("childs: " + tree2.getModel().getChildCount(treeObject));
            
            int childNum = tree2.getModel().getChildCount(treeObject);
            for(int i=0; i<childNum; i++) {
            	Object childObject = tree2.getModel().getChild(treeObject, i);
            	DefaultMutableTreeNode child = (DefaultMutableTreeNode) childObject;
            	
            	String childPath = getItemsPath(child);
            	
            	try {
            		WorkspaceItem item;
            		if(workspaceMap.containsKey(childPath)) {
            			item = workspaceMap.get(childPath);
            		} else {
            			item =  ws.getItemByPath(childPath);
            			workspaceMap.put(childPath, item);
            			
            		}
					
					if(item.isFolder()) {
						System.out.println(childPath + " is folder!");
						
						List<WorkspaceItem> children;
						if(workspaceChildrenMap.containsKey(item)) {
							children = workspaceChildrenMap.get(item);
						}else {
							WorkspaceFolder folder = (WorkspaceFolder) item;
							children = folder.getChildren();
							
							workspaceChildrenMap.put(item, children);
						}
						
						for(WorkspaceItem grandChild:children){
							String name = grandChild.getName();
							String grandChildPath = childPath + "/" + name;
							if(!mytree.containsKey(grandChildPath)) {
								mytree.put(grandChildPath, new DefaultMutableTreeNode(name));
								mytree.get(childPath).add(mytree.get(grandChildPath));
							}
							if(!workspaceMap.containsKey(grandChildPath)) {
								workspaceMap.put(grandChildPath, grandChild);
							}
							
							System.out.println(grandChildPath);
						}
					}else {
						System.out.println(childPath + " is NOT folder!");
					}
				}  catch (InternalErrorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ItemNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
		}

		public void treeCollapsed(TreeExpansionEvent event) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////
	
	
	private class ListenForMouse implements MouseListener{

		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			/*if(e.getSource() == tree2) {
				if(e.getClickCount() == 2 ) {
					Object treeObject = tree2.getLastSelectedPathComponent();
					// Cast the Object into a DefaultMutableTreeNode
					DefaultMutableTreeNode file = (DefaultMutableTreeNode) treeObject;
					
					for (Enumeration enumValue = file.children(); enumValue.hasMoreElements(); ) {
						String fileName = enumValue.nextElement().toString();
						//System.out.println(fileName);
						
						TreeNode[] pathNodes = file.getPath();
			            // Cycle through the TreeNodes
			            String path = "";
			            for(TreeNode indivNodes: pathNodes){
			            	path += "/" + indivNodes;
			            }
			            path += "/" + fileName;
			            //System.out.println(path);
			            
			            if(Objects.equals(fileName, "Folder1") || Objects.equals(fileName, "Folder1_1")) {
				            WorkspaceFolder folder;
							try {
								folder = (WorkspaceFolder) ws.getItemByPath(path);
								List<WorkspaceItem> children = folder.getChildren();
								
								for(WorkspaceItem child:children){
									String name;
									name = child.getName();
									String path2 = path + "/" + name;
									if(!mytree.containsKey(path2)) {
										mytree.put(path2, new DefaultMutableTreeNode(name));
										mytree.get(path).add(mytree.get(path2));
									}
								}
							} catch (ItemNotFoundException e2) {
								// TODO Auto-generated catch block
								e2.printStackTrace();
							} catch (InternalErrorException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
			            }
					}
				}
			}*/
			
		}

		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public class HelloRunnable implements Runnable {

	    public void run() {
	    	while(true) {
	    		System.out.println("Hello from a thread!");
	    		
	    		try {
	    			for (Map.Entry<String, Calendar> entry : syncFile.entrySet()) {
						//System.out.println(entry.getKey() + " : " + entry.getValue());
						String itemID = entry.getKey();
						WorkspaceItem item = ws.getItem(itemID);
						
						Calendar date1 = entry.getValue();
						Calendar date2 = item.getLastModificationTime();
						if(date1.compareTo(date2) < 0) {
							//////	OUTDATED - UPDATE	////////
							System.out.println("ITEM OUTDATED");
						}
					}
	    			
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ItemNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InternalErrorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    }
	}
	
}
