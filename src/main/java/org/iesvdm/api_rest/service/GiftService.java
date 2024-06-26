package org.iesvdm.api_rest.service;

import org.iesvdm.api_rest.domain.Gift;
import org.iesvdm.api_rest.domain.Wedding;
import org.iesvdm.api_rest.exception.EntityNotFoundException;
import org.iesvdm.api_rest.exception.NotCouplingIdException;
import org.iesvdm.api_rest.repository.GiftRepository;
import org.iesvdm.api_rest.repository.WeddingRepository;
import org.iesvdm.api_rest.util.PaginationTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GiftService {

    @Autowired
    GiftRepository giftRepository;
    @Autowired
    WeddingRepository weddingRepository;

    public List<Gift> all(){return this.giftRepository.findAll();}

    // Pagination of All data by Wedding id:
    public Map<String, Object> allByWeddingId(long id, int page, int size){
        Pageable paginator = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Gift> pageAll = this.giftRepository.findByWedding_Id(id, paginator);

        System.out.println("Inside allByWeddingId method in Gift Service");
        System.out.println("PAGINATED RESULT" + pageAll);
        return PaginationTool.createPaginatedResponseMap(pageAll, "gifts");
    }

    // Find Wedding's gifts by filter and return paginated:
    public Map<String, Object> findByWeddingIdAndFilter(long id, int page, int size, String filter){
        Pageable paginator = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Gift> pageFiltered = this.giftRepository
                .findGiftsByNameContainingIgnoreCaseAndWedding_Id(filter, id, paginator);

        return PaginationTool.createPaginatedResponseMap(pageFiltered, "gifts");
    }

    public Gift one(Long id){
        return this.giftRepository.findById(id)
                .orElseThrow(()-> new EntityNotFoundException(id, Gift.class));
    }

    public Gift save(Long id, Gift gift){
        Wedding wedding = weddingRepository.findById(id).get();
        gift.setWedding(wedding);
        return this.giftRepository.save(gift);
    }

    public Gift replace(Long id, Gift gift){
        return this.giftRepository.findById(id).map(m -> {
            if (id.equals(gift.getId())){
                Wedding wedding = weddingRepository.findWeddingByGifts_Id(id);
                gift.setWedding(wedding);
                return this.giftRepository.save(gift);
            }
            else throw new NotCouplingIdException(id, gift.getId(), Gift.class);
        }).orElseThrow(()-> new EntityNotFoundException(id, Gift.class));
    }

    public void delete(Long id){
        this.giftRepository.findById(id).map(m -> {this.giftRepository.delete(m);
                    return m;})
                .orElseThrow(()-> new EntityNotFoundException(id, Gift.class));
    }
}
