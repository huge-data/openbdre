package com.wipro.ats.bdre.wgen.dag;

import com.wipro.ats.bdre.md.beans.ProcessInfo;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by su324335 on 8/25/16.
 */
public class DAGDataQualityTaskNode extends GenericActionNode {
    private static final Logger LOGGER = Logger.getLogger(DAGImportTaskNode.class);

    private ProcessInfo processInfo = new ProcessInfo();
    private DAGTaskNode taskNode = null;

    /**
     * This constructor is used to set node id and process information.
     *
     * @param taskNode An instance of ActionNode class which a workflow triggers the execution of a task.
     */
    public DAGDataQualityTaskNode(DAGTaskNode taskNode) {
        setId(taskNode.getId());
        processInfo = taskNode.getProcessInfo();
        this.taskNode = taskNode;
    }

    public ProcessInfo getProcessInfo() {
        return processInfo;
    }


    public String getName() {

        String nodeName = "data_quality" + getId() + "_" + processInfo.getProcessName().replace('-', '_').replace(' ', '_');
        return nodeName.substring(0, Math.min(nodeName.length(), 45));

    }


    @Override
    public String getDAG() {
        if (this.getProcessInfo().getParentProcessId() == 0) {
            return "";
        }
        DAGNode fileListNode = null;
        for (DAGNode dagNode : taskNode.getContainingNodes()) {
            if (dagNode instanceof DAGLOFTaskNode) {
                fileListNode = dagNode;
            }
        }
        String homeDir = System.getProperty("user.home");
        String jobInfoFile = homeDir+"/bdre/airflow/"+processInfo.getParentProcessId().toString()+"_jobInfo.txt";
        StringBuilder ret = new StringBuilder();

        ret.append( "\nwith open('"+jobInfoFile+"','a+') as propeties_register_file:\n"+
                        "\tfor line in propeties_register_file:\n"+
                        "\t\tfile_info = line.split('::',2)\n"+
                        "\t\tdict[file_info[0]] = file_info[1].replace('\\n','')\n"+


                        "with open('"+homeDir + "/bdre/airflow/etldriverInfo.txt"+"','a+') as etl_driver_file:\n"+
                        "\tfor line in etl_driver_file:\n"+
                        "\t\tfile_info = line.split('::',2)\n"+
                        "\t\tdict[file_info[0]] = file_info[1].replace('\\n','')\n"+

                        "\ndef "+ getName()+"_pc():\n" +
                        "\tcommand='java -cp "+homeDir+"/bdre/lib/dq/*:"+homeDir+"/bdre/lib/*/*  com.wipro.ats.bdre.dq.DQMain --process-id "+ getId().toString()+"  --source-file-path  \'+dict[\"getETLDriverInfo.getFileList()\"]+\'  --destination-directory /raw/\'+dict[\"initJobInfo.getInstanceExecId()\"]  \n"+
                        "\tbash_output = subprocess.Popen(command,shell=True, stdin=subprocess.PIPE, stdout=subprocess.PIPE )\n" +
                        "\tout,err = bash_output.communicate()\n"+
                        "\tlogger.info(\"out is \"+str(out))\n"+
                        "\tlogger.info(\"err is \"+str(err))\n"+
                        "\tif(bash_output.returncode != 0):\n" +
                        "\t\treturn 'dummy_"+getName() +"'\n" +
                        "\telse:\n" +
                        "\t\treturn '"+getToNode().getName() +"'\n" +

                        "\ndef f_"+ getName()+"():\n" +
                        "\t"+ getName()+".set_downstream("+ getToNode().getName()+")\n" +
                        "\t"+ getName()+".set_downstream(dummy_"+ getName()+")\n" +
                        "\t"+ "dummy_"+ getName()+".set_downstream("+getTermNode().getName() +")\n"+
                        getName()+" = BranchPythonOperator(task_id='"+getName()+"', python_callable="+getName()+"_pc, dag=dag)\n"+
                        "dummy_"+ getName()+" = DummyOperator(task_id ='"+"dummy_"+ getName()+"',dag=dag)\n"
        );


        try {

            FileWriter fw = new FileWriter(homeDir+"/defFile.txt", true);
            fw.write("\nf_"+getName()+"()");
            fw.close();
        }
        catch (IOException e){
            LOGGER.debug("error occured:  " + e);
        }


        return  ret.toString();

    }



}