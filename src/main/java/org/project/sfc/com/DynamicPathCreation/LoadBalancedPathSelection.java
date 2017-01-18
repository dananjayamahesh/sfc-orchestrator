package org.project.sfc.com.DynamicPathCreation;

import org.openbaton.catalogue.mano.common.Ip;
import org.openbaton.catalogue.mano.descriptor.NetworkForwardingPath;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.NetworkServiceRecord;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFForwardingGraphRecord;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.sfc.com.SfcHandler.SFC;
import org.project.sfc.com.SfcImpl.Broker.SfcBroker;
import org.project.sfc.com.SfcImpl.ODL_SFC_driver.ODL_SFC.NeutronClient;
import org.project.sfc.com.SfcModel.SFCdict.SFPdict;
import org.project.sfc.com.SfcModel.SFCdict.VNFdict;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created by mah on 11/14/16.
 */
public class LoadBalancedPathSelection {
  NeutronClient NC;
  SFC sfcc_db = org.project.sfc.com.SfcHandler.SFC.getInstance();
  org.project.sfc.com.SfcInterfaces.SFC SFC_driver;
  org.project.sfc.com.SfcInterfaces.SFCclassifier SFC_Classifier_driver;
    SfcBroker broker = new SfcBroker();

  public LoadBalancedPathSelection(String type) throws IOException {


    this.SFC_driver = broker.getSFC(type);
    this.SFC_Classifier_driver = broker.getSfcClassifier(type);
  }

  public void ReadjustVNFsAllocation(VirtualNetworkFunctionRecord vnfr)  {
    System.out.println("----------------------------[Readjust VNFs Allocation] Start ----------------------------------");

        List<SFC.SFC_Data> Chains_data=new ArrayList<>();
    List<VNFdict> VNF_instance_dicts=new ArrayList<>();
    HashMap<String,Double> PrevVNFTrafficLoad=new HashMap<String, Double>();

    HashMap<String, SFC.SFC_Data> All_SFCs = sfcc_db.getAllSFCs();
    HashMap<String, SFC.SFC_Data> Involved_SFCs =new HashMap<String, SFC.SFC_Data>();

    if (All_SFCs != null) {
      System.out.println("[ ALL SFCs number ] =  " + All_SFCs.size());
    }
    int total_size_SFCs=All_SFCs.size();
    System.out.println("[Read Just VNFs Allocation] Get Involved SFCs for this VNF ");

    //Get Involved SFCs for this VNF
    Iterator it = All_SFCs.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry SFCdata_counter = (Map.Entry) it.next();
      HashMap<Integer, VNFdict> VNFs = All_SFCs.get(SFCdata_counter.getKey()).getChainSFs();
      Iterator vnfs_count = VNFs.entrySet().iterator();
      while (vnfs_count.hasNext()) {
        Map.Entry VNFcounter = (Map.Entry) vnfs_count.next();
        if (VNFs.get(VNFcounter.getKey()).getType().equals(vnfr.getType())){
          Involved_SFCs.put(All_SFCs.get(SFCdata_counter.getKey()).getRspID(),All_SFCs.get(SFCdata_counter.getKey()));
          System.out.println("[LB Path Selection] Involved SFCs :  " + All_SFCs.get(SFCdata_counter.getKey()).getRspID());

        }

      }
    }


    int total_size_VNF_instances=0;

    for (VirtualDeploymentUnit vdu_x : vnfr.getVdu()) {
      for (VNFCInstance vnfc_instance : vdu_x.getVnfc_instance()) {
        PrevVNFTrafficLoad.put(vnfc_instance.getHostname(),0.0);
        System.out.println("[PrevVNFTAFFICload] VNF instance name : " + vnfc_instance.getHostname()+ " Load: "+PrevVNFTrafficLoad.get(vnfc_instance.getHostname()));

        total_size_VNF_instances++;
      }
    }
    System.out.println("[LB Path Selection] Total Size of VNF instances : " + total_size_VNF_instances);

    HashMap<String, SFC.SFC_Data> SelectedSFCs =new HashMap<String, SFC.SFC_Data>();

    System.out.println("[LB Path Selection] Levels of Selections = " + String.valueOf(Math.floor(total_size_SFCs/total_size_VNF_instances)));

for(int counter1=0;counter1<=Math.floor(total_size_SFCs/total_size_VNF_instances);counter1++) {

  for (int x = 0; x < total_size_VNF_instances; x++) {

    if (Involved_SFCs.size() > 0) {
      double MaxLoad = 0;
      String RSPID = null;

      Iterator c = Involved_SFCs.entrySet().iterator();
      while (c.hasNext()) {
        Map.Entry SFCKey = (Map.Entry) c.next();

        if (Involved_SFCs.get(SFCKey.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getPathTrafficLoad() >=
            MaxLoad) {
          System.out.println("This Test_SFC:  " +Involved_SFCs.get(SFCKey.getKey()).getRspID() + " has Load = "+ Involved_SFCs.get(SFCKey.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getPathTrafficLoad() +" higher than the Max Load < "+ MaxLoad+" >");

          if(RSPID!=null) {
            System.out.println("[LB Path Selection]    RSPID is not empty  " );

            if (SelectedSFCs.containsKey(RSPID)) {
              SelectedSFCs.remove(RSPID);
              System.out.println("[LB Path Selection] Selected Test_SFC is Removed, Test_SFC RSP ID:  " + RSPID);

            }
          }
          SelectedSFCs.put(Involved_SFCs.get(SFCKey.getKey()).getRspID(), Involved_SFCs.get(SFCKey.getKey()));
          MaxLoad = Involved_SFCs.get(SFCKey.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getPathTrafficLoad();
          RSPID = SFCKey.getKey().toString();
          System.out.println("[LB Path Selection] Selected Test_SFC at round " + counter1 +"  is  "+Involved_SFCs.get(SFCKey.getKey()) + " with Max Load = "+MaxLoad + " and RSP ID = "+RSPID);

        }
      }
      Involved_SFCs.remove(RSPID);
      System.out.println("[ Remove from the Involved SFCs ]  The RSP ID is  " + RSPID);
    }

  }

  // Get which Test_SFC should be selected
  int size_selected_sfcs = SelectedSFCs.size();

  for (int i = 0; i < size_selected_sfcs; i++) {
    Iterator counter = SelectedSFCs.entrySet().iterator();

    double Load = 0;
    int recentQoS = 0;
    String RSPID = null;
    String SelectedVNFinstance = null;
    SFC.SFC_Data selectedChain = null;
    System.out.println("[ Selected Chains ]  size:  " +SelectedSFCs.size() );

    while (counter.hasNext()) {
      Map.Entry involved_SFC_data_counter = (Map.Entry) counter.next();
      System.out.println("[ Selected Chain ]  QoS:  " +SelectedSFCs.get(involved_SFC_data_counter.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getQoS() );
      System.out.println("[ Recent   QoS ] :  " +recentQoS );

      if (SelectedSFCs.get(involved_SFC_data_counter.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getQoS
          () > recentQoS) {
        System.out.println("This Test_SFC:  " +SelectedSFCs.get(involved_SFC_data_counter.getKey()).getRspID() + " has QoS Priority= "+ SelectedSFCs.get(involved_SFC_data_counter.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getQoS
            () +" HIGHER than the best QoS Priority < "+ recentQoS+" >");

        Load = SelectedSFCs.get(involved_SFC_data_counter.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getPathTrafficLoad();
        selectedChain = SelectedSFCs.get(involved_SFC_data_counter.getKey());
        recentQoS =
            SelectedSFCs.get(involved_SFC_data_counter.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0)
                        .getQoS();
        RSPID = involved_SFC_data_counter.getKey().toString();

        System.out.println("[ Selected Chain ]  [Current Qos > Last QoS] Selected Chain:  " +selectedChain + " Last QoS= "+recentQoS+ " RSP ID= "+RSPID);

      } else if (SelectedSFCs.get(involved_SFC_data_counter.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0)
                             .getQoS() == recentQoS) {
        System.out.println("This Test_SFC:  " +SelectedSFCs.get(involved_SFC_data_counter.getKey()).getRspID() + " has QoS Priority= "+ SelectedSFCs.get(involved_SFC_data_counter.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getQoS
            () +" EQUAL to the best QoS Priority < "+ recentQoS+" >");
        if (SelectedSFCs.get(involved_SFC_data_counter.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getPathTrafficLoad() > Load) {
          selectedChain = SelectedSFCs.get(involved_SFC_data_counter.getKey());
          System.out.println("This Test_SFC:  " +SelectedSFCs.get(involved_SFC_data_counter.getKey()).getRspID() + " has Load= "+ SelectedSFCs.get(involved_SFC_data_counter.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getPathTrafficLoad() +" HIGHER than the MaxLoad < "+ Load+" >");
          Load = SelectedSFCs.get(involved_SFC_data_counter.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getPathTrafficLoad();
          recentQoS =
              SelectedSFCs.get(involved_SFC_data_counter.getKey()).getSFCdictInfo().getSfcDict().getPaths().get(0).getQoS();
          RSPID = involved_SFC_data_counter.getKey().toString();

        }
      }
    }
    System.out.println("[ Selected Chain ]   Selected Chain:  " +selectedChain + " Best QoS= "+recentQoS+ " RSP ID= "+RSPID);

    SelectedSFCs.remove(RSPID);

      double MinLoad = Double.MAX_VALUE ;

      //Allocate the selected Test_SFC to lowest Load of the VNF instances
      Iterator vnf_TL_counter = PrevVNFTrafficLoad.entrySet().iterator();
      boolean Selected=false;
      while (vnf_TL_counter.hasNext()) {

        Map.Entry VNF_key = (Map.Entry) vnf_TL_counter.next();
        System.out.println("[Allocate the selected Test_SFC to lowest VNF instance Load ]  Load="+ PrevVNFTrafficLoad.get(VNF_key.getKey())+" Min Load= "+MinLoad);

        if (PrevVNFTrafficLoad.get(VNF_key.getKey()) < MinLoad) {


          MinLoad = PrevVNFTrafficLoad.get(VNF_key.getKey());

          SelectedVNFinstance = VNF_key.getKey().toString();

          Selected=true;
          System.out.println("[Min Load ]="+MinLoad+ " [Selected VNF instance]= " +SelectedVNFinstance);


        }
      }
      if(Selected==true) {
        double prev_load=PrevVNFTrafficLoad.get(SelectedVNFinstance);
        PrevVNFTrafficLoad.put(SelectedVNFinstance, prev_load+Load);
        System.out.println("[ Alocate Selected Chain to lowest VNF load ]  Min Load:  " +MinLoad + " VNF instance= "+SelectedVNFinstance);

      }



    Iterator it_new = All_SFCs.entrySet().iterator();
    VNFdict SelectedVNFdict = null;
    while (it_new.hasNext()) {

      Map.Entry SFCdata_counter = (Map.Entry) it_new.next();
      HashMap<Integer, VNFdict> VNFs = All_SFCs.get(SFCdata_counter.getKey()).getChainSFs();
      Iterator vnfs_count = VNFs.entrySet().iterator();
      while (vnfs_count.hasNext()) {

        Map.Entry VNFcounter = (Map.Entry) vnfs_count.next();
        System.out.println("VNF name: "+VNFs.get(VNFcounter.getKey()).getName()+" [Selected VNF instance]"+SelectedVNFinstance);

        if (VNFs.get(VNFcounter.getKey()).getName().equals(SelectedVNFinstance)) {

          SelectedVNFdict = VNFs.get(VNFcounter.getKey());
          System.out.println("[Selected VNFdict]"+SelectedVNFdict);

          break;
        }
      }
    }
    System.out.println("[ CreatChain ]  chain RSPID  " +selectedChain.getRspID() + " VSelected VNF instance= "+SelectedVNFdict.getName());

    Chains_data.add(selectedChain);
    VNF_instance_dicts.add(SelectedVNFdict);
  }
}

    for(int x=0;x<Chains_data.size();x++){
      CreateChain(Chains_data.get(x), VNF_instance_dicts.get(x));

    }
    System.out.println("----------------------------[Readjust VNFs Allocation] END ----------------------------------");

  }

private void CreateChain(SFC.SFC_Data Chain_Data, VNFdict VNF_instance)  {

  System.out.println("[ LB Path Selection < Create Chain > ]  ");

  HashMap<Integer, VNFdict> VNFs = Chain_Data.getChainSFs();
  Iterator count = VNFs.entrySet().iterator();
  System.out.println("[ LB Path Selection < Create Chain > ] SIZE of VNFs  " + VNFs.size());

  while (count.hasNext()) {

    Map.Entry VNFcounter = (Map.Entry) count.next();
    System.out.println("[ LB Path Selection < Create Chain > ] chain data VNF type= "+VNFs.get(VNFcounter.getKey()).getType()+"VNF instance TYPE= "+VNF_instance.getType());

    if (VNFs.get(VNFcounter.getKey()).getType().equals(VNF_instance.getType())){
      System.out.println("[ LB Path Selection < Create Chain > Found the same TYPE  ");

      int position = Integer.valueOf(VNFcounter.getKey().toString()).intValue();

      VNFs.remove(position);

      VNFs.put(position, VNF_instance);
      System.out.println("[ LB Path Selection < Create Chain > ] DONE  ");

      break;

    }
  }
  System.out.println("Set Chain VNFs");

  Chain_Data.setChainSFs(VNFs);
  System.out.println("Done");

  List<SFPdict> newPaths=new ArrayList<>();
  SFPdict newPath= Chain_Data.getSFCdictInfo().getSfcDict().getPaths().get(0);
  newPath.setPath_SFs(VNFs);
  double PathTrafficLoad=Chain_Data.getSFCdictInfo().getSfcDict().getPaths().get(0).getPathTrafficLoad();
  newPath.setOldTrafficLoad(PathTrafficLoad);

  newPaths.add(0,newPath);
  System.out.println("Get Test_SFC DICT and set the Path");

  Chain_Data.getSFCdictInfo().getSfcDict().setPaths(newPaths);
  System.out.println("Done");

  System.out.println("Create the new path");

  String new_instance_id =
      SFC_driver.CreateSFP(
          Chain_Data.getSFCdictInfo(),
          VNFs);
  System.out.println("NEW Path readjusted ] " + new_instance_id);

  String SFCC_name =
      SFC_Classifier_driver.Create_SFC_Classifer(
          Chain_Data.getClassifierInfo(), new_instance_id);

  System.out.println("[NEW Classifier updated ] " + SFCC_name);
  System.out.println("[Update SFCC DB] " + Chain_Data.getRspID().substring(5));
  String IDx=Chain_Data.getRspID().substring(5);
  System.out.println("[IDx] ="+ IDx );

  String VNFFGR_ID=IDx.substring(IDx.indexOf('-')+1);
  System.out.println("[VNFFGR ID] ="+ VNFFGR_ID );

  sfcc_db.update(
      VNFFGR_ID,
      new_instance_id,
      Chain_Data.getSfccName(),
      Chain_Data.getSFCdictInfo().getSfcDict().getSymmetrical(),
      VNFs,
      Chain_Data.getSFCdictInfo(),
      Chain_Data.getClassifierInfo());
  System.out.println("[Update SFCC DB] is done !!" );

}

}