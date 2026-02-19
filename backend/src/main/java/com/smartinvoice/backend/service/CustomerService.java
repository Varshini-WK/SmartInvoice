package com.smartinvoice.backend.service;

import com.smartinvoice.backend.domain.Customer;
import com.smartinvoice.backend.dto.CreateCustomerRequest;
import com.smartinvoice.backend.dto.CustomerResponse;
import com.smartinvoice.backend.dto.UpdateCustomerRequest;
import lombok.RequiredArgsConstructor;
import com.smartinvoice.backend.mapper.CustomerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartinvoice.backend.repository.CustomerRepository;
import com.smartinvoice.backend.tenant.BusinessContext;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    private UUID getBusinessId() {
        String businessIdStr = BusinessContext.getBusinessId();
        if (businessIdStr == null) {
            throw new IllegalStateException("Missing X-Business-ID header");
        }
        return UUID.fromString(businessIdStr);
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {

        UUID businessId = getBusinessId();

        Customer customer = new Customer();
        customer.setBusinessId(businessId);
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());

        return CustomerMapper.toResponse(
                customerRepository.save(customer)
        );
    }

    public List<CustomerResponse> list() {

        UUID businessId = getBusinessId();

        return customerRepository
                .findAllByBusinessId(businessId)
                .stream()
                .map(CustomerMapper::toResponse)
                .toList();
    }

    public CustomerResponse get(UUID id) {

        UUID businessId = getBusinessId();

        Customer customer = customerRepository
                .findByBusinessIdAndId(businessId, id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return CustomerMapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(UUID id, UpdateCustomerRequest request) {

        UUID businessId = getBusinessId();

        Customer customer = customerRepository
                .findByBusinessIdAndId(businessId, id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());

        return CustomerMapper.toResponse(customer);
    }

    @Transactional
    public void delete(UUID id) {

        UUID businessId = getBusinessId();

        Customer customer = customerRepository
                .findByBusinessIdAndId(businessId, id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customerRepository.delete(customer);
    }
}
