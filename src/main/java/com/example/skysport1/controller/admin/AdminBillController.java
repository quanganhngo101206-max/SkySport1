package com.example.skysport1.controller.admin;

import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.exception.ResourceNotFoundException;
import com.example.skysport1.service.BillService;
import com.example.skysport1.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/bills")
@RequiredArgsConstructor
@Slf4j
public class AdminBillController {

	private final BillService billService;
	private final StaffService staffService;

	@GetMapping
	public String list(@RequestParam(defaultValue = "0") int page,
						@RequestParam(defaultValue = "10") int size,
						@RequestParam(required = false) Integer status,
						Model model) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Bill> pageResult;
		if (status != null) {
			pageResult = billService.findByStatusPaged(status, pageable);
		} else {
			pageResult = billService.findAllPaged(pageable);
		}

		model.addAttribute("bills", pageResult.getContent());
		model.addAttribute("page", pageResult.getNumber());
		model.addAttribute("size", size);
		model.addAttribute("totalPages", pageResult.getTotalPages());
		model.addAttribute("status", status);
		model.addAttribute("title", "Danh sách đơn hàng");
		model.addAttribute("pageContent", "admin/bill/list");
		log.info("Loaded {} bills on page {} with status filter: {}", pageResult.getContent().size(), page, status);
		return "layouts/adminlte/layout";
	}

	@GetMapping("/{id}")
	public String view(@PathVariable String id, Model model, RedirectAttributes ra) {
		try {
			Bill bill = billService.findById(id);
			model.addAttribute("bill", bill);
			model.addAttribute("title", "Chi tiết đơn hàng");
			model.addAttribute("pageContent", "admin/bill/detail");
			return "layouts/adminlte/layout";
		} catch (Exception e) {
			log.error("Error loading bill {}: {}", id, e.getMessage(), e);
			ra.addFlashAttribute("error", "Không tìm thấy đơn hàng: " + id);
			return "redirect:/admin/bills";
		}
	}

	@GetMapping("/{id}/confirm")
	public String confirmForm(@PathVariable String id) {
		return "redirect:/admin/bills/" + id;
	}

	@PostMapping("/{id}/confirm")
	public String confirm(@PathVariable String id,
						   @RequestParam(required = false) String note,
						   Authentication auth,
						   RedirectAttributes ra) {
		try {
			log.info("AdminBillController.confirm (POST) called id={}, auth={}",
					id, auth != null ? auth.getName() : "null");
			Staff staff = staffService.findByAccountUsername(auth.getName());
			billService.confirm(id, staff.getAccount().getId(), note);
		} catch (ResourceNotFoundException e) {
			log.warn("AdminBillController.confirm ResourceNotFound id={}, auth={}",
					id, auth != null ? auth.getName() : "null");
			billService.confirm(id, null, note);
		} catch (Exception e) {
			log.error("AdminBillController.confirm failed id={}, err={}", id, e.getMessage(), e);
			ra.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/bills/" + id;
	}

	// handle trailing slash variant: /admin/bills/{id}/confirm/
	@PostMapping("/{id}/confirm/")
	public String confirmTrailingSlash(@PathVariable String id,
										 @RequestParam(required = false) String note,
										 Authentication auth,
										 RedirectAttributes ra) {
		log.info("AdminBillController.confirm (POST trailing slash) called id={}, auth={}",
				id, auth != null ? auth.getName() : "null");
		return confirm(id, note, auth, ra);
	}

	@PostMapping("/{id}/ship")
	public String ship(@PathVariable String id,
						@RequestParam(required = false) String note,
						Authentication auth,
						RedirectAttributes ra) {
		try {
			Staff staff = staffService.findByAccountUsername(auth.getName());
			billService.startShipping(id, staff.getAccount().getId(), note);
		} catch (ResourceNotFoundException e) {
			billService.startShipping(id, null, note);
		} catch (Exception e) {
			ra.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/bills/" + id;
	}

	@PostMapping("/{id}/deliver")
	public String deliver(@PathVariable String id,
						   @RequestParam(required = false) String note,
						   Authentication auth,
						   RedirectAttributes ra) {
		try {
			Staff staff = staffService.findByAccountUsername(auth.getName());
			billService.markDelivered(id, staff.getAccount().getId(), note);
		} catch (ResourceNotFoundException e) {
			billService.markDelivered(id, null, note);
		} catch (Exception e) {
			ra.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/bills/" + id;
	}

	@PostMapping("/{id}/cancel")
	public String cancel(@PathVariable String id,
						  @RequestParam(required = false) String note,
						  Authentication auth,
						  RedirectAttributes ra) {
		try {
			Staff staff = staffService.findByAccountUsername(auth.getName());
			billService.cancel(id, staff.getAccount().getId(), note);
		} catch (ResourceNotFoundException e) {
			billService.cancel(id, null, note);
		} catch (Exception e) {
			ra.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/bills/" + id;
	}

	@PostMapping("/{id}/complete")
	public String complete(@PathVariable String id,
							@RequestParam(required = false) String note,
							Authentication auth,
							RedirectAttributes ra) {
		try {
			Staff staff = staffService.findByAccountUsername(auth.getName());
			billService.complete(id, staff.getAccount().getId(), note);
		} catch (ResourceNotFoundException e) {
			billService.complete(id, null, note);
		} catch (Exception e) {
			ra.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/bills/" + id;
	}
}