/** * * Copyright &copy; 2015-2020 <a href="https://github.com/gaowenhui/RocketSpringCloud">JeeSpring</a> All rights reserved.. */
package com.jeespring.modules.job.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeespring.modules.sys.service.SysConfigService;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.common.collect.Lists;
import com.jeespring.common.utils.DateUtils;
import com.jeespring.common.config.Global;
import com.jeespring.common.persistence.Page;
import com.jeespring.common.web.AbstractBaseController;
import com.jeespring.common.utils.StringUtils;
import com.jeespring.common.utils.excel.ExportExcel;
import com.jeespring.common.utils.excel.ImportExcel;
import com.jeespring.modules.job.entity.SysJob;
import com.jeespring.modules.job.service.SysJobService;
/**
 * 定时任务调度Controller
 * @author gaowh
 * @version 2018-08-16
 */
@Controller
@RequestMapping(value = "${adminPath}/job/sysJob")
public class SysJobController extends AbstractBaseController {

	@Autowired
	private SysJobService sysJobService;
	@Autowired
	private SysConfigService sysConfigService;

	@ModelAttribute
	public SysJob get(@RequestParam(required=false) String id) {
		SysJob entity = null;
		if (StringUtils.isNotBlank(id)){
			entity = sysJobService.getCache(id);
			//entity = sysJobService.get(id);
		}
		if (entity == null){
			entity = new SysJob();
		}
		return entity;
	}

	/**
	 * 定时任务调度统计页面
	 */
	@RequiresPermissions("job:sysJob:total")
	@RequestMapping(value = {"total"})
	public String totalView(SysJob sysJob, HttpServletRequest request, HttpServletResponse response, Model model) {
		total(sysJob,request,response,model);
		return "modules/job/sysJobTotal";
	}
	private void total(SysJob sysJob, HttpServletRequest request, HttpServletResponse response, Model model) {
			if(StringUtils.isEmpty(sysJob.getTotalType())){
			sysJob.setTotalType("%Y-%m-%d");
		}
		//X轴的数据
		List<String> xAxisData= new ArrayList<String>();
		//Y轴的数据
		Map<String,List<Double>> yAxisData = new HashMap<String,List<Double>>();
		List<Double> countList = new ArrayList<Double>();
		List<Double> sumList = new ArrayList<Double>();
		if(sysJob.getOrderBy()==""){
			sysJob.setOrderBy("totalDate");
		}
		List<SysJob> list = sysJobService.totalCache(sysJob);
		//List<SysJob> list = sysJobService.total(sysJob);
		model.addAttribute("list", list);
		for(SysJob sysJobItem:list){
			//x轴数据
			xAxisData.add( sysJobItem.getTotalDate());
			countList.add(Double.valueOf(sysJobItem.getTotalCount()));
		}
		yAxisData.put("数量", countList);
	    request.setAttribute("xAxisData", xAxisData);
		request.setAttribute("yAxisData", yAxisData);
		model.addAttribute("sumTotalCount", list.stream().mapToInt(SysJob::getTotalCount).sum());

		//饼图数据
		Map<String,Object> orientData= new HashMap<String,Object>();
		for(SysJob sysJobItem:list){
			orientData.put(sysJobItem.getTotalDate(), sysJobItem.getTotalCount());
		}
		model.addAttribute("orientData", orientData);
	}
	@RequiresPermissions("job:sysJob:total")
	@RequestMapping(value = {"totalMap"})
	public String totalMap(SysJob sysJob, HttpServletRequest request, HttpServletResponse response, Model model) {
		if(StringUtils.isEmpty(sysJob.getTotalType())){
			sysJob.setTotalType("%Y-%m-%d");
		}
		List<SysJob> list = sysJobService.totalCache(sysJob);
		//List<SysJob> list = sysJobService.total(sysJob);
		model.addAttribute("sumTotalCount", list.stream().mapToInt(SysJob::getTotalCount).sum());
		model.addAttribute("list", list);
		return "modules/job/sysJobTotalMap";
	}

	/**
	 * 定时任务调度列表页面
	 */
	@RequiresPermissions("job:sysJob:list")
	@RequestMapping(value = {"list", ""})
	public String list(SysJob sysJob, HttpServletRequest request, HttpServletResponse response, Model model) {
		Page<SysJob> page = sysJobService.findPageCache(new Page<SysJob>(request, response), sysJob);
		//Page<SysJob> page = sysJobService.findPage(new Page<SysJob>(request, response), sysJob);
		model.addAttribute("page", page);
		sysJob.setOrderBy("totalDate");
		total(sysJob,request,response,model);
		return "modules/job/sysJobList";
	}

	/**
	 * 定时任务调度列表页面
	 */
	@RequiresPermissions("job:sysJob:list")
	@RequestMapping(value = {"listVue"})
	public String listVue(SysJob sysJob, HttpServletRequest request, HttpServletResponse response, Model model) {
		Page<SysJob> page = sysJobService.findPageCache(new Page<SysJob>(request, response), sysJob);
		//Page<SysJob> page = sysJobService.findPage(new Page<SysJob>(request, response), sysJob);
		model.addAttribute("page", page);
		return "modules/job/sysJobListVue";
	}

	/**
	 * 定时任务调度列表页面
	 */
	//RequiresPermissions("job:sysJob:select")
	@RequestMapping(value = {"select"})
	public String select(SysJob sysJob, HttpServletRequest request, HttpServletResponse response, Model model) {
		Page<SysJob> page = sysJobService.findPageCache(new Page<SysJob>(request, response), sysJob);
		//Page<SysJob> page = sysJobService.findPage(new Page<SysJob>(request, response), sysJob);
		model.addAttribute("page", page);
		return "modules/job/sysJobSelect";
	}

	/**
	 * 查看，增加，编辑定时任务调度表单页面
	 */
	@RequiresPermissions(value={"job:sysJob:view","job:sysJob:add","job:sysJob:edit"},logical=Logical.OR)
	@RequestMapping(value = "form")
	public String form(SysJob sysJob, Model model, HttpServletRequest request, HttpServletResponse response) {
		model.addAttribute("action", request.getParameter("action"));
		model.addAttribute("sysJob", sysJob);
		if(request.getParameter("ViewFormType")!=null && request.getParameter("ViewFormType").equals("FormTwo"))
			return "modules/job/sysJobFormTwo";
		return "modules/job/sysJobForm";
	}

	/**
	 * 保存定时任务调度
	 */
	@RequiresPermissions(value={"job:sysJob:add","job:sysJob:edit"},logical=Logical.OR)
	@RequestMapping(value = "save")
	public String save(SysJob sysJob, Model model, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response) {
		if(sysConfigService.isDemoMode()){
			addMessage(redirectAttributes, sysConfigService.isDemoModeDescription());
			return "redirect:" + adminPath + "/job/sysJob/?repage";
		}

		if (!beanValidator(model, sysJob)){
			return form(sysJob, model,request,response);
		}
		sysJobService.save(sysJob);
		addMessage(redirectAttributes, "保存定时任务调度成功");
		return "redirect:"+Global.getAdminPath()+"/job/sysJob/?repage";
	}

	/**
	 * 任务调度立即执行一次
	 */
	@RequiresPermissions(value={"job:sysJob:add","job:sysJob:edit"},logical=Logical.OR)
	@RequestMapping(value = "run")
	public String run(SysJob job,RedirectAttributes redirectAttributes,HttpServletRequest request, HttpServletResponse response)
	{
		sysJobService.run(job);
		addMessage(redirectAttributes, "任务调度立即执行一次成功");
		return "redirect:"+Global.getAdminPath()+"/job/sysJob/?repage";
	}

	/**
	 * 任务调度状态修改
	 */
	@RequiresPermissions(value={"job:sysJob:add","job:sysJob:edit"},logical=Logical.OR)
	@RequestMapping(value = "changeStatus")
	public String changeStatus(SysJob job,RedirectAttributes redirectAttributes,HttpServletRequest request, HttpServletResponse response)
	{
		sysJobService.changeStatus(job);
		addMessage(redirectAttributes, "任务调度状态修改成功");
		return "redirect:"+Global.getAdminPath()+"/job/sysJob/?repage";
	}

	/**
	 * 删除定时任务调度
	 */
	@RequiresPermissions("job:sysJob:del")
	@RequestMapping(value = "delete")
	public String delete(SysJob sysJob, RedirectAttributes redirectAttributes) {
		if(sysConfigService.isDemoMode()){
			addMessage(redirectAttributes, sysConfigService.isDemoModeDescription());
			return "redirect:" + adminPath + "/job/sysJob/?repage";
		}
		sysJobService.delete(sysJob);
		addMessage(redirectAttributes, "删除定时任务调度成功");
		return "redirect:"+Global.getAdminPath()+"/job/sysJob/?repage";
	}

	/**
	 * 删除定时任务调度（逻辑删除，更新del_flag字段为1,在表包含字段del_flag时，可以调用此方法，将数据隐藏）
	 */
	@RequiresPermissions(value={"job:sysJob:del","job:sysJob:delByLogic"},logical=Logical.OR)
	@RequestMapping(value = "deleteByLogic")
	public String deleteByLogic(SysJob sysJob, RedirectAttributes redirectAttributes) {
		if(sysConfigService.isDemoMode()){
			addMessage(redirectAttributes, sysConfigService.isDemoModeDescription());
			return "redirect:" + adminPath + "/job/sysJob/?repage";
		}
		sysJobService.deleteByLogic(sysJob);
		addMessage(redirectAttributes, "逻辑删除定时任务调度成功");
		return "redirect:"+Global.getAdminPath()+"/job/sysJob/?repage";
	}

	/**
	 * 批量删除定时任务调度
	 */
	@RequiresPermissions("job:sysJob:del")
	@RequestMapping(value = "deleteAll")
	public String deleteAll(String ids, RedirectAttributes redirectAttributes) {
		if(sysConfigService.isDemoMode()){
			addMessage(redirectAttributes, sysConfigService.isDemoModeDescription());
			return "redirect:" + adminPath + "/job/sysJob/?repage";
		}
		String idArray[] =ids.split(",");
		for(String id : idArray){
			sysJobService.delete(sysJobService.get(id));
		}
		addMessage(redirectAttributes, "删除定时任务调度成功");
		return "redirect:"+Global.getAdminPath()+"/job/sysJob/?repage";
	}

	/**
	 * 批量删除定时任务调度（逻辑删除，更新del_flag字段为1,在表包含字段del_flag时，可以调用此方法，将数据隐藏）
	 */
	@RequiresPermissions(value={"job:sysJob:del","job:sysJob:delByLogic"},logical=Logical.OR)
	@RequestMapping(value = "deleteAllByLogic")
	public String deleteAllByLogic(String ids, RedirectAttributes redirectAttributes) {
		if(sysConfigService.isDemoMode()){
			addMessage(redirectAttributes, sysConfigService.isDemoModeDescription());
			return "redirect:" + adminPath + "/job/sysJob/?repage";
		}
		String idArray[] =ids.split(",");
		for(String id : idArray){
			sysJobService.deleteByLogic(sysJobService.get(id));
		}
		addMessage(redirectAttributes, "删除定时任务调度成功");
		return "redirect:"+Global.getAdminPath()+"/job/sysJob/?repage";
	}

	/**
	 * 导出excel文件
	 */
	@RequiresPermissions("job:sysJob:export")
    @RequestMapping(value = "export", method=RequestMethod.POST)
    public String exportFile(SysJob sysJob, HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes) {
		try {
            String fileName = "定时任务调度"+DateUtils.getDate("yyyyMMddHHmmss")+".xlsx";
            Page<SysJob> page = sysJobService.findPage(new Page<SysJob>(request, response, -1), sysJob);
    		new ExportExcel("定时任务调度", SysJob.class).setDataList(page.getList()).write(response, fileName).dispose();
    		return null;
		} catch (Exception e) {
			addMessage(redirectAttributes, "导出定时任务调度记录失败！失败信息："+e.getMessage());
		}
		return "redirect:"+Global.getAdminPath()+"/job/sysJob/?repage";
    }

	/**
	 * 导入Excel数据

	 */
	@RequiresPermissions("job:sysJob:import")
    @RequestMapping(value = "import", method=RequestMethod.POST)
    public String importFile(MultipartFile file, RedirectAttributes redirectAttributes) {
		try {
			int successNum = 0;
			ImportExcel ei = new ImportExcel(file, 1, 0);
			List<SysJob> list = ei.getDataList(SysJob.class);
			for (SysJob sysJob : list){
				sysJobService.save(sysJob);
			}
			addMessage(redirectAttributes, "已成功导入 "+successNum+" 条定时任务调度记录");
		} catch (Exception e) {
			addMessage(redirectAttributes, "导入定时任务调度失败！失败信息："+e.getMessage());
		}
		return "redirect:"+Global.getAdminPath()+"/job/sysJob/?repage";
    }
	
	/**
	 * 下载导入定时任务调度数据模板
	 */
	@RequiresPermissions("job:sysJob:import")
    @RequestMapping(value = "import/template")
    public String importFileTemplate(HttpServletResponse response, RedirectAttributes redirectAttributes) {
		try {
            String fileName = "定时任务调度数据导入模板.xlsx";
    		List<SysJob> list = Lists.newArrayList(); 
    		new ExportExcel("定时任务调度数据", SysJob.class, 1).setDataList(list).write(response, fileName).dispose();
    		return null;
		} catch (Exception e) {
			addMessage(redirectAttributes, "导入模板下载失败！失败信息："+e.getMessage());
		}
		return "redirect:"+Global.getAdminPath()+"/job/sysJob/?repage";
    }

}